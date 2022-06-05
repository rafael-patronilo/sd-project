package tp1.common.services;

import tp1.client.rest.RestFilesClient;
import tp1.client.soap.SoapFilesClient;
import tp1.common.ServerUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.common.exceptions.UnexpectedErrorException;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.KafkaUtils;
import tp1.kafka.operations.*;
import tp1.kafka.sync.SyncPoint;
import tp1.server.rest.RESTFilesServer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Files services
 */
public class LocalFilesService implements FilesService {
    // the path where files will be stored
    private static final String STORAGE_PATH = "./%s";
    private static Logger Log = Logger.getLogger(LocalFilesService.class.getName());
    private static final List<String> TOPICS = Arrays.asList(KafkaUtils.DIR_FILES_TOPIC);

    private final SyncPoint<String> syncPoint = SyncPoint.getInstance();

    // The files currently hosted in this server
    private Set<String> files = new HashSet<>();
    private long version = -1L;

    // The Kafka subscriber for this service
    private KafkaSubscriber subscriber = KafkaSubscriber
            .createSubscriber(KafkaUtils.KAFKA_BROKERS, TOPICS, KafkaUtils.FROM_BEGINNING);

    public LocalFilesService(){
        /* TODO do we need this?
        // maps file id to its respective original server (for replication)
        Map<String, String> fileIdToServer = new HashMap<>();
        // Obtain operations that were executed before this server restarted
        subscriber.consumeOnceOperation((operation, offset)->{
            if(operation instanceof FileOperation){
                if(operation instanceof Create op){
                    if(!op.original().equals(uri) &&
                            op.replicas().contains(uri)) {
                        fileIdToServer.put(op.fileId(), op.original());
                    }
                } else if(operation instanceof Move op){
                    if(!op.original().equals(uri) &&
                            op.replicas().contains(uri)) {
                        fileIdToServer.put(op.fileId(), op.original());
                    }
                } else if (operation instanceof Edit op){
                    if(!op.original().equals(uri) &&
                            fileIdToServer.containsKey(uri)) {
                        fileIdToServer.put(op.fileId(), op.original());
                    }
                } else if (operation instanceof Delete op){
                    fileIdToServer.remove(op.fileId());
                }
            }
        });
        for(Map.Entry<String, String> toReplicate : fileIdToServer.entrySet()){
            replicate(toReplicate.getValue(), toReplicate.getKey());
        }*/
        subscriber.startWithOp(false, this::executeOperation);
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException {
        Log.info("writeFile : " + fileId);
        try{
            FileOutputStream out = new FileOutputStream(pathTo(fileId));
            out.write(data);
            out.close();
            files.add(fileId);
        } catch (IOException e) {
            Log.info(String.format("throw UnexpectedError: IO Exception (%s)", e.getMessage()));
            throw new UnexpectedErrorException();
        }
    }

    @Override
    public void deleteFile(String fileId, String token) throws InvalidFileLocationException {
        Log.info("deleteFile : " + fileId);
        File file = new File(pathTo(fileId));
        if(!file.delete()){
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        }
        files.remove(fileId);
    }

    @Override
    public byte[] getFile(String fileId, String token, long version) throws UnexpectedErrorException, InvalidFileLocationException {
        Log.info("getFile : " + fileId + "\n\t version: " + version);
        syncPoint.waitForVersion(version);
        byte[] data = null;
        try {
            FileInputStream in = new FileInputStream(pathTo(fileId));
            data = in.readAllBytes();
            in.close();
        } catch (FileNotFoundException e) {
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        } catch (IOException e) {
            Log.info(String.format("throw UnexpectedError: IO Exception (%s)", e.getMessage()));
            throw new UnexpectedErrorException();
        }
        return data;
    }

    /**
     * Builds the path to the file given its id
     *
     * @param fileId the file's id
     * @return the file's path
     */
    static String pathTo(String fileId) {
        return String.format(LocalFilesService.STORAGE_PATH, fileId);
    }

    private void replicate(String serverUri, String fileId) {
        Log.info("Replicating file " + fileId + " from " + serverUri);
        FilesServerClient client;
        if(serverUri.endsWith("rest")){
            client = new RestFilesClient(serverUri);
        } else {
            client = new SoapFilesClient(serverUri);
        }
        try (FileOutputStream out = new FileOutputStream(pathTo(fileId))){
            byte[] data = client.getFile(fileId, "");
            out.write(data);
        } catch (RequestTimeoutException | InvalidFileLocationException | IOException e) {
            Log.severe("Unexpected exception of type " + e.getClass().getName() + " during replication");
        }
    }

    private void executeOperation(Operation operation, long offset){
        String uri = ServerUtils.getUri();
        Log.info("Operation received: " + operation.toString());
        if(operation instanceof FileOperation){
            if(operation instanceof Create op){
                if(!op.original().equals(uri) &&
                        op.replicas().contains(uri)) {
                    replicate(op.original(), op.fileId());
                    files.add(op.fileId());
                }
            } else if(operation instanceof Move op){
                if(!op.original().equals(uri)){
                    boolean isReplica = op.replicas().contains(uri);
                    if(isReplica && !files.contains(op.fileId())){
                        replicate(op.original(), op.fileId());
                    } else if(!isReplica && files.contains(op.fileId())){
                        //TODO delete file?
                    }
                }
            } else if (operation instanceof Edit op){
                if(!op.original().equals(uri) &&
                        files.contains(op.fileId())) {
                    replicate(op.original(), op.fileId());
                }
            } else if (operation instanceof Delete op){
                if(files.contains(op.fileId())){
                    new File(pathTo(op.fileId())).delete();
                    files.remove(op.fileId());
                }
            }
        }
        syncPoint.setVersion(offset);
        Log.info("Version set to " + offset);
    }

}
