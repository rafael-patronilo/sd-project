package tp1.common.services;

import tp1.client.rest.RestFilesClient;
import tp1.client.soap.SoapFilesClient;
import tp1.common.ServerUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.InvalidTokenException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.common.exceptions.UnexpectedErrorException;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.KafkaUtils;
import tp1.kafka.operations.*;
import tp1.kafka.sync.SyncPoint;
import tp1.tokens.PermanentToken;
import tp1.tokens.TemporaryToken;
import tp1.tokens.Token;
import tp1.tokens.TokenManager;
import java.util.*;
import java.util.logging.Logger;

public abstract class BaseFilesService implements FilesService{

    private Logger Log;

    private static final List<String> TOPICS = Arrays.asList(KafkaUtils.DIR_FILES_TOPIC);

    protected Set<String> files = new HashSet<>();
    private KafkaSubscriber subscriber = KafkaSubscriber
            .createSubscriber(KafkaUtils.KAFKA_BROKERS, TOPICS, KafkaUtils.FROM_BEGINNING);
    protected final SyncPoint<String> syncPoint = SyncPoint.getInstance();

    protected BaseFilesService(Logger log){
        Log = log;
        subscriber.startWithOp(false, this::executeOperation);
    }

    private void replicate(String serverUri, String fileId) {
        Log.info("Replicating file " + fileId + " from " + serverUri);
        FilesServerClient client;
        if(serverUri.endsWith("rest")){
            client = new RestFilesClient(serverUri);
        } else {
            client = new SoapFilesClient(serverUri);
        }
        try{
            byte[] data = client.getFile(fileId);
            writeFile(fileId, data);
        } catch (RequestTimeoutException | InvalidFileLocationException | UnexpectedErrorException e) {
            Log.severe("Unexpected exception of type " + e.getClass().getName() + " during replication");
        }
    }

    private void executeOperation(Operation operation, long offset){
        String uri = ServerUtils.getUri();
        Log.info("Operation received: " + operation.opName());
        if(operation instanceof FileOperation){
            if(operation instanceof Create op){
                if (op.original().equals(uri)){
                    files.add(op.fileId());
                } else if(op.replicas().contains(uri)) {
                    replicate(op.original(), op.fileId());
                    files.add(op.fileId());
                }
            } else if(operation instanceof Move op){
                if(!op.original().equals(uri)){
                    boolean isReplica = op.replicas().contains(uri);
                    if(isReplica && !files.contains(op.fileId())){
                        replicate(op.original(), op.fileId());
                    } else if(!isReplica && files.contains(op.fileId())){
                        try {
                            deleteFile(op.fileId());
                        } catch (InvalidFileLocationException ignored){}
                    }
                }
            } else if (operation instanceof Edit op){
                if(!op.original().equals(uri) &&
                        files.contains(op.fileId())) {
                    replicate(op.original(), op.fileId());
                }
            } else if (operation instanceof Delete op){
                if(files.contains(op.fileId())){
                    try {
                        deleteFile(op.fileId());
                    } catch (InvalidFileLocationException ignored){}
                    files.remove(op.fileId());
                }
            }
        }
        syncPoint.setVersion(offset);
        Log.info("Version set to " + offset);
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException, InvalidTokenException {
        Log.info("writeFile : " + fileId + "\n token: " + token);
        validateToken(token, fileId, true);
        writeFile(fileId, data);
    }

    protected abstract void writeFile(String fileId, byte[] data) throws UnexpectedErrorException;

    @Override
    public void deleteFile(String fileId, String token) throws InvalidFileLocationException, InvalidTokenException {
        Log.info("deleteFile : " + fileId + "\n token: " + token);
        validateToken(token, fileId, true);
        deleteFile(fileId);
    }
    protected abstract void deleteFile(String fileId) throws InvalidFileLocationException;

    protected void validateToken(String serialized, String fileId)throws InvalidTokenException{
        validateToken(serialized, fileId, false);
    }

    protected void validateToken(String serialized, String fileId, boolean requirePermanent) throws InvalidTokenException{
        Token token = TokenManager.checkToken(serialized);
        if(token == null) {
            Log.info("Invalid or expired token");
            throw new InvalidTokenException();
        } if(token instanceof TemporaryToken temp){
            if(requirePermanent) {
                Log.info("Permanent token required for this operation.");
                throw new InvalidTokenException();
            }
            if(!Objects.equals(temp.getFileId(), fileId)){
                Log.info("Token for another file passed.");
                throw new InvalidTokenException();
            }
            Log.info("Valid temporary token for " + fileId + "; Expires in " +
                    temp.currentValidity() + "ms (Current time " + System.currentTimeMillis() + ")");
        } else if (!(token instanceof PermanentToken)){
            throw new IllegalStateException("Unrecognized token.");
        }
    }
}
