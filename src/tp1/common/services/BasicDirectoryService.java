package tp1.common.services;

import tp1.api.FileInfo;
import tp1.client.ClientUtils;
import tp1.client.rest.RestFilesClient;
import tp1.client.rest.RestUsersClient;
import tp1.client.soap.SoapFilesClient;
import tp1.client.soap.SoapUsersClient;
import tp1.common.ServerUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.clients.UsersServerClient;
import tp1.common.exceptions.*;
import tp1.kafka.KafkaPublisher;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.KafkaUtils;
import tp1.kafka.operations.*;
import tp1.kafka.sync.SyncPoint;
import tp1.server.MulticastServiceDiscovery;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Directory services
 */
public class BasicDirectoryService implements DirectoryService {

    private static final Logger Log = Logger.getLogger(BasicDirectoryService.class.getName());

    private final SyncPoint<String> syncPoint = SyncPoint.getInstance();
    private static final List<String> TOPICS = Arrays.asList(KafkaUtils.DIR_FILES_TOPIC);
    private final KafkaPublisher publisher;

    private final String replicaId = "yes";
    private UsersServerClient usersServer = null;

    // Priority queue with file servers prioritizing the ones with less occupied space
    private final PriorityQueue<FileServerMonitor> filesServers = new PriorityQueue<>();

    // Maps the userId to the respective (concurrent safe) directory
    private final Map<String, Map<String, FileReference>> directories = new HashMap<>();

    private KafkaSubscriber subscriber;

    // The file id for the next file written
    private AtomicLong nextFileId = new AtomicLong();

    /**
     * Associates a file server to the respective amount of used storage
     */
    private static class FileServerMonitor implements Comparable<FileServerMonitor> {
        FilesServerClient server;

        // used storage in bytes
        int usedStorage;

        FileServerMonitor(FilesServerClient server){
            this.server = server;
            this.usedStorage = 0;
        }

        @Override
        public int compareTo(FileServerMonitor other) {
            return Integer.compare(this.usedStorage, other.usedStorage);
        }
    }

    /**
     * Contains the information and location of a certain file
     */
    private static class FileReference{
        // file id on the server
        String fileId;

        // server where it is stored
        FileServerMonitor[] servers;

        // meta information about the file
        FileInfo info;

        // the last replica where an operation was performed
        int lastReplica = 0;

        // size in bytes of the file
        int size;

        public FileReference(String fileId, FileServerMonitor[] servers, FileInfo info, int size) {
            this.fileId = fileId;
            this.servers = servers;
            this.info = info;
            this.size = size;
        }

        /**
         * Picks the replica next to lastReplica
         *
         * This is used to distribute packet load amongst the replicas and to move to the next
         * replica when one fails.
         * @return the next replica
         */
        int shitfReplica(){
            lastReplica = (lastReplica + 1) % servers.length;
            return lastReplica;
        }

        /**
         * Returns a Set with the URI of each replica
         * @return the set of URIs
         */
        private Set<String> URIs(){
            Set<String> uris = new HashSet<>(servers.length);
            for(FileServerMonitor monitor : servers){
                uris.add(monitor.server.getURI());
            }
            return uris;
        }

    }

    public BasicDirectoryService(){
        MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
        // listener for file servers
        Consumer<String> filesListener = (String uri) ->{
            FilesServerClient proxy = null;
            if(uri.endsWith("rest")) {
                proxy = new RestFilesClient(uri);
            }
            else if(uri.endsWith("soap")){
                proxy = new SoapFilesClient(uri);
            }
            synchronized (filesServers) {
                filesServers.add(new FileServerMonitor(proxy));
            }
        };
        // add file servers already discovered
        for(String uri : discovery.discoveredServices(FilesService.NAME)){
            filesListener.accept(uri);
        }
        discovery.listenForServices(FilesService.NAME, filesListener);

        Set<String> users = discovery.discoveredServices(UsersService.NAME);
        // listener for the users server
        Consumer<String> userListener = (String uri)->{
            if(usersServer == null){
                if(uri.endsWith("rest")) {
                    usersServer = new RestUsersClient(uri);
                }
                else if (uri.endsWith("soap")) {
                    usersServer = new SoapUsersClient(uri);
                }
            }
        };
        // if users server already discovered add it, otherwise add listener
        if(users.isEmpty()){
            discovery.listenForServices(UsersService.NAME, userListener);
        } else {
            userListener.accept(users.iterator().next());
        }
        publisher = KafkaPublisher.createPublisher(KafkaUtils.KAFKA_BROKERS);
        subscriber = KafkaSubscriber
                .createSubscriber(KafkaUtils.KAFKA_BROKERS, TOPICS, KafkaUtils.FROM_BEGINNING);
        subscriber.startWithOp(false, this::executeOperation);
    }

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password)
            throws UnexpectedErrorException, RequestTimeoutException,
            IncorrectPasswordException, InvalidUserIdException {
        Log.info("writeFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        Map<String, FileReference> directory = getDirectory(userId);
        FileReference reference = directory.get(filename);

        if (reference == null) { // case new file on the directory
            FileInfo info = new FileInfo(userId, filename, null, new HashSet<>());
            String fileId = String.valueOf(nextFileId.getAndIncrement());
            Log.info(String.format("mapped %s/%s to file id %s", userId, filename, fileId));

            // attempt to send file to a file server
            FileServerMonitor counter = sendFile(data, fileId);
            if(counter == null) {
                Log.info("throw RequestTimeout: out of files servers");
                throw new RequestTimeoutException();
            }
            Log.info("File sent");
            FileServerMonitor[] replicas = pickReplicas(counter, data.length);
            Log.info("Replicas picked");

            reference = new FileReference(fileId, replicas, info, data.length);
            reference.info.setFileURL(reference.servers[0].server.getFileDirectUrl(fileId));

            long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId, new Create(userId, filename, fileId, reference.size,
                    reference.servers[0].server.getURI(), reference.URIs(), info));
            syncPoint.waitForVersion(version);
        } else { // case file already on the directory (overwrite)
            FileServerMonitor sentTo = sendFileBack(data, reference);
            // sizeDifference = newSize - oldSize (=) newSize = oldSize + sizeDifference
            int sizeDifference = data.length - reference.size;
            // Report edit
            long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId, new Edit(reference.info.getOwner(),
                    reference.info.getFilename(), reference.fileId,
                    sizeDifference, sentTo.server.getURI()));
            syncPoint.waitForVersion(version);
        }

        return reference.info;
    }

    @Override
    public void deleteFile(String filename, String userId, String password)
            throws InvalidFileLocationException, RequestTimeoutException,
            IncorrectPasswordException, InvalidUserIdException {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);

        // delete the file on the directory
        FileReference removing = getDirectory(userId).get(filename);
        if(removing == null){
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }

        // delete the file on the file server (asynchronously)
        long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId, new Delete(userId, filename, removing.fileId));
        syncPoint.waitForVersion(version);
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password)
            throws InvalidFileLocationException, RequestTimeoutException,
            IncorrectPasswordException, InvalidUserIdException {
        Log.info("shareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileReference reference = getDirectory(userId).get(filename);
        if(reference == null) {
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }

        validatePassword(userId, password);
        if(!userId.equals(userIdShare)) {
            long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId,
                    new Share(userId, filename, userIdShare));
            syncPoint.waitForVersion(version);
        }
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password)
            throws InvalidFileLocationException, RequestTimeoutException,
            IncorrectPasswordException, InvalidUserIdException {
        Log.info("unshareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileReference reference = getDirectory(userId).get(filename);
        if(reference == null) {
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }

        validatePassword(userId, password);
        if(!userId.equals(userIdShare)) {
            long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId,
                    new Unshare(userId, filename, userIdShare));
            syncPoint.waitForVersion(version);
        }
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password,
                          boolean tryRedirect, long version) throws InvalidFileLocationException,
            NoAccessException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        Log.info("getFile : filename = " + filename + "; userId = " + userId + "; accUserId = "
                + accUserId + "; password = " + password);
        validatePassword(accUserId, password);
        boolean isOwner = userId.equals(accUserId);
        if(!isOwner){ // if it's owner, validatePassword already validated the user
            validateUser(userId);
        }
        FileReference reference;
        reference = getDirectory(userId).get(filename);
        if(reference == null) {
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }
        if(!reference.info.getSharedWith().contains(accUserId) && !isOwner) {
            Log.info("throw NoAccessException: user doesn't have access to file");
            throw new NoAccessException();
        }
        FilesServerClient files = reference.servers[reference.shitfReplica()].server;

        // redirecting to the files server, if possible, is faster than transferring the file
        if(tryRedirect) {
            Log.info("Attempting to redirect to files server");
            files.redirectToGetFile(reference.fileId, "", version);
            // If the redirect succeeds, execution won't reach this point
            Log.info("Failed to redirect");
        }

        return files.getFile(reference.fileId, "", version);
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        Log.info("lsFile : userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        List<FileInfo> returning = new ArrayList<>();
        synchronized (directories) {
            for (Map<String, FileReference> directory : directories.values()) {
                for (FileReference reference : directory.values()) {
                    FileInfo info = reference.info;
                    if (info.getOwner().equals(userId) || info.getSharedWith().contains(userId)) {
                        returning.add(info);
                    }
                }
            }
        }
        return returning;
    }

    @Override
    public void deleteDirectory(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException {
        Log.info("deleteDirectory : userId = " + userId + "; password = " + password);
        try {
            usersServer.getUser(userId, password);
        } catch (InvalidUserIdException ignored) {
            Log.info("User does not exist; deleting anyway");
        }
        Map<String, FileReference> directory = getDirectory(userId);
        for (FileReference reference : directory.values()) {
            publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId,
                    new Delete(reference.info.getOwner(), reference.info.getFilename(), reference.fileId));
        }
    }

    /**
     * Validates that the userId and password are valid
     * @param userId the user id to validate
     * @param password the user's password
     * @throws RequestTimeoutException if the users server doesn't respond
     * @throws IncorrectPasswordException if the password is incorrect
     * @throws InvalidUserIdException if there is no user with that id
     */
    private void validatePassword(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        usersServer.getUser(userId, password);
    }
    
    private FileServerMonitor sendFileBack(byte[] data, FileReference reference) throws RequestTimeoutException {
        FileServerMonitor[] originalCounters = reference.servers;

        int firstServer = -1;
        // Goes over the servers one by one until one succeeds, preferring the next servers in line
        for(int i = reference.shitfReplica(); i != firstServer; i = reference.shitfReplica()){
            if(firstServer == -1)
                firstServer = i;
            int maxRetries = filesServers.size() - i == 1 ? ClientUtils.MAX_RETRIES : 1;
            try {
                Log.info("Attempting to send file to one of its replicas");
                originalCounters[i].server.writeFile(reference.fileId, data, "", maxRetries);
                return originalCounters[i];
            } catch (RequestTimeoutException ignored){}
        }

        Log.severe("timed out");
        // attempt writing to another file server (ignoring the one we've
        //already tried)
        FileServerMonitor sentTo = sendFile(data, reference.fileId, reference.servers);
        if(sentTo == null) {
            Log.info("throw RequestTimeout: out of files servers");
            throw new RequestTimeoutException();
        }
        originalCounters[firstServer] = sentTo;

        //Report that the file was moved from a server to another
        long version = publisher.publish(KafkaUtils.DIR_FILES_TOPIC, replicaId, new Move(reference.info.getOwner(),
                reference.info.getFilename(), reference.fileId,
                sentTo.server.getURI(), reference.URIs()));
        syncPoint.setVersion(version);


        // TODO replace with wait for version?
        reference.size = data.length;
        reference.info.setFileURL(sentTo.server.getFileDirectUrl(reference.fileId));
        return sentTo;
    }

    /**
     * Attempts to send a file to a file server,
     * prioritizing file server with less used storage
     * and moving to another file server on timeout
     * @param data the contents of the file to send
     * @param fileId the file's id
     * @param toIgnore file servers that will be ignored
     * @return the file server that it was sent to or null if they all failed
     */
    private synchronized FileServerMonitor sendFile(byte[] data, String fileId, FileServerMonitor[] toIgnore){
        for (FileServerMonitor monitor : toIgnore){
            filesServers.remove(monitor);
        }
        FileServerMonitor result = sendFile(data, fileId);
        filesServers.addAll(Arrays.asList(toIgnore));
        return result;
    }

    /**
     * Attempts to send a file to a file server,
     * prioritizing file server with less used storage
     * and moving to another file server on timeout
     * @param data the contents of the file to send
     * @param fileId the file's id
     * @return the file server that it was sent to or null if they all failed
     */
    private synchronized FileServerMonitor sendFile(byte[] data, String fileId) {
        FileServerMonitor counter;
        FileServerMonitor polled = filesServers.poll();
        if(polled == null){
            Log.severe("No file servers or all file servers timed out");
            return null;
        }

        try {
            int maxRetries = 1;
            if(filesServers.size() == 0 ||
                    (filesServers.size() == 1)){
                maxRetries = ClientUtils.MAX_RETRIES;
                Log.info("Attempting to send file to last files server");
            }
            polled.server.writeFile(fileId, data, "", maxRetries);
            polled.usedStorage += data.length;
            counter = polled;
        } catch (RequestTimeoutException e) {
            Log.severe("timed out");
            counter = sendFile(data, fileId);
        }
        filesServers.add(polled);
        return counter;
    }

    /**
     * Selects the first FilesService.NUMBER_OF_REPLICAS with the least occupied space
     * @param original the file server that already contains the file (so it won't be selected again)
     * @param size the size of the file
     * @return
     */
    private synchronized FileServerMonitor[] pickReplicas(FileServerMonitor original, int size){
        int count = Math.min(filesServers.size(), FilesService.NUMBER_OF_REPLICAS);
        FileServerMonitor[] replicas = new FileServerMonitor[count];
        replicas[0] = original;
        boolean originalPolled = false;
        for (int i = 1; i < count; i++) {
            FileServerMonitor server = filesServers.poll();
            if(server == original) {
                originalPolled = true;
                server = filesServers.poll();
            }
            replicas[i] = server;
            assert replicas[i] != null; //count is at most the number of files
            replicas[i].usedStorage += size;
        }
        if(originalPolled)
            filesServers.add(original);
        for (int i = 1; i < count; i++) {
            filesServers.add(replicas[i]);
        }
        return replicas;
    }

    private synchronized Map<String, FileReference> getDirectory(String userId){
        return directories.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Validates that a given user id exists
     * @param userId the user id to validate
     * @throws RequestTimeoutException if the users' server doesn't respond
     * @throws InvalidUserIdException if the user id is invalid
     */
    private void validateUser(String userId) throws RequestTimeoutException, InvalidUserIdException {
            try {
                if(!usersServer.hasUser(userId)){
                    Log.info("throw InvalidUserIdException: userId doesn't exist");
                    throw new InvalidUserIdException();
                }
            } catch (RequestTimeoutException e){
                Log.info("throw RequestTimeoutException: hasUser request timed out");
                throw new RequestTimeoutException();
            }
    }

    private FileServerMonitor serverFromURI(String uri){
        for(FileServerMonitor monitor : filesServers){
            if(monitor.server.getURI().equals(uri)){
                return monitor;
            }
        }
         return null;
    }

    private FileServerMonitor[] getReplicaArray(Set<String> uris, String original){
        FileServerMonitor[] servers = new FileServerMonitor[uris.size()];
        int i = 0;
        for(String uri : uris){
            FileServerMonitor monitor = serverFromURI(uri);
            if(uri.equals(original)){
                servers[i++] = servers[0];
                servers[0] = monitor;
            } else{
                servers[i++] = monitor;
            }
        }
        return servers;
    }

    private void executeOperation(Operation operation, long offset){
        Log.info("Operation received: " + operation.opName());
        String filename = operation.filename();
        String userId = operation.userId();
        int sizeDifference = operation.sizeDifference();
        FileReference reference;
        if(operation instanceof Create op){
            reference = new FileReference(op.fileId(), getReplicaArray(op.replicas(), op.original()),
                    op.fileInfo(), sizeDifference);
            if(sizeDifference != 0){
                for(FileServerMonitor monitor : reference.servers){
                    monitor.usedStorage += sizeDifference;
                }
            }
            getDirectory(userId).put(filename, reference);

        } else{
            reference = getDirectory(userId).get(filename);
            if(operation instanceof Delete){
                sizeDifference = reference.size;
            }
            reference.size += operation.sizeDifference();
            if(sizeDifference != 0){
                for(FileServerMonitor monitor : reference.servers){
                    monitor.usedStorage += sizeDifference;
                }
            }
            if(operation instanceof Move op){
                reference.servers = getReplicaArray(op.replicas(), op.original());
                reference.info.setFileURL(reference.servers[0].server.getFileDirectUrl(op.fileId()));
            } else if (operation instanceof Delete){
                getDirectory(userId).remove(filename);
            } else if(operation instanceof Share op){
                reference.info.getSharedWith().add(op.sharingWith());
            } else if(operation instanceof Unshare op){
                reference.info.getSharedWith().remove(op.sharedWith());
            }
        }
        syncPoint.setVersion(offset);
        Log.info("Version set to " + offset);
    }

}
