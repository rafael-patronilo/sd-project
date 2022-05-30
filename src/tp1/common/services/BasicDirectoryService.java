package tp1.common.services;

import tp1.api.FileInfo;
import tp1.client.ClientUtils;
import tp1.client.rest.RestFilesClient;
import tp1.client.rest.RestUsersClient;
import tp1.client.soap.SoapFilesClient;
import tp1.client.soap.SoapUsersClient;
import tp1.common.WithHeader;
import tp1.common.clients.FilesServerClient;
import tp1.common.clients.UsersServerClient;
import tp1.common.exceptions.*;
import tp1.server.MulticastServiceDiscovery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Directory services
 */
public class BasicDirectoryService implements DirectoryService {
    private static final Logger Log = Logger.getLogger(BasicDirectoryService.class.getName());
    private UsersServerClient usersServer = null;

    // Priority queue with file servers prioritizing the ones with less occupied space
    private final PriorityQueue<FileServerMonitor> filesServers = new PriorityQueue<>();

    // Maps the userId to the respective (concurrent safe) directory
    private final Map<String, Map<String, FileReference>> directories = new HashMap<>();

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
        FileServerMonitor server;

        // meta information about the file
        FileInfo info;

        // size in bytes of the file
        int size;

        public FileReference(String fileId, FileServerMonitor server, FileInfo info, int size) {
            this.fileId = fileId;
            this.server = server;
            this.info = info;
            this.size = size;
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
    }

    @Override
    public WithHeader<FileInfo> writeFile(String filename, byte[] data, String userId, String password)
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

            reference = new FileReference(fileId, counter, info, data.length);
            reference.info.setFileURL(counter.server.getFileDirectUrl(fileId));
            directory.put(filename, reference);
        } else { // case file already on the directory (overwrite
            FileServerMonitor originalCounter = reference.server;
            FilesServerClient originalServer = originalCounter.server;
            try { // prefer writing to the original file server
                Log.info("Attempting to send file to its original file server");
                int maxRetries = filesServers.size() == 1 ? ClientUtils.MAX_RETRIES : 1;
                originalServer.writeFile(reference.fileId, data, "", maxRetries);
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                // attempt writing to another file server (ignoring the one we've
                //already tried)
                reference.server = sendFile(data, reference.fileId, originalCounter);
                if(reference.server == null) {
                    reference.server = originalCounter;
                    Log.info("throw RequestTimeout: out of files servers");
                    throw new RequestTimeoutException();
                }

                //Try to clean the file from the old server
                synchronized (filesServers) {
                    filesServers.remove(originalCounter);
                    originalCounter.usedStorage -= reference.size;
                    filesServers.add(originalCounter);
                }
                originalServer.deleteFileAsync(reference.fileId, "");

                reference.size = data.length;
                reference.info.setFileURL(originalCounter.server.getFileDirectUrl(reference.fileId));
            }
        }

        return new WithHeader<>(
                DirectoryService.LAST_FILE_OP_HEADER, /*TODO*/"", reference.info);
    }

    @Override
    public WithHeader<Object> deleteFile(String filename, String userId, String password)
            throws InvalidFileLocationException, RequestTimeoutException,
            IncorrectPasswordException, InvalidUserIdException {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);

        // delete the file on the directory
        FileReference removed = getDirectory(userId).remove(filename);
        if(removed == null){
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }

        // delete the file on the file server (asynchronously)
        removed.server.server.deleteFileAsync(removed.fileId, "");
        synchronized (filesServers) {
            filesServers.remove(removed.server);
            removed.server.usedStorage -= removed.size;
            filesServers.add(removed.server);
        }
        return new WithHeader<>(
                DirectoryService.LAST_FILE_OP_HEADER, /*TODO*/"", null);
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
        if(!userId.equals(userIdShare))
            reference.info.getSharedWith().add(userIdShare);
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
        reference.info.getSharedWith().remove(userIdShare);
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password, boolean tryRedirect) throws InvalidFileLocationException, NoAccessException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
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
        FilesServerClient files = reference.server.server;

        // redirecting to the files server, if possible, is faster than transferring the file
        if(tryRedirect) {
            Log.info("Attempting to redirect to files server");
            files.redirectToGetFile(reference.fileId, "");
            // If the redirect succeeds, execution won't reach this point
            Log.info("Failed to redirect");
        }

        return reference.server.server.getFile(reference.fileId, "");
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
    public WithHeader<Object> deleteDirectory(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException {
        Log.info("deleteDirectory : userId = " + userId + "; password = " + password);
        try {
            usersServer.getUser(userId, password);
        } catch (InvalidUserIdException ignored) {
            Log.info("User does not exist; deleting anyway");
        }
        Map<String, FileReference> directory = getDirectory(userId);
        for (FileReference reference : directory.values()) {
            reference.server.server.deleteFileAsync(reference.fileId, "");
        }
        directory.clear();
        return new WithHeader<>(
                DirectoryService.LAST_FILE_OP_HEADER, /*TODO*/"", null);
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

    /**
     * Attempts to send a file to a file server,
     * prioritizing file server with less used storage
     * and moving to another file server on timeout
     * @param data the contents of the file to send
     * @param fileId the file's id
     * @return the file server that it was sent to or null if they all failed
     */
    private FileServerMonitor sendFile(byte[] data, String fileId){
        return sendFile(data, fileId, null);
    }

    /**
     * Attempts to send a file to a file server,
     * prioritizing file server with less used storage
     * and moving to another file server on timeout
     * @param data the contents of the file to send
     * @param fileId the file's id
     * @param toIgnore a file server that will be skipped if encountered
     * @return the file server that it was sent to or null if they all failed
     */
    private synchronized FileServerMonitor sendFile(byte[] data, String fileId, FileServerMonitor toIgnore) {
        FileServerMonitor counter;
        FileServerMonitor polled = filesServers.poll();
        if(polled == null){
            Log.severe("No file servers or all file servers timed out");
            return null;
        }

        if(polled == toIgnore){
            counter = sendFile(data, fileId, toIgnore);
        } else {
            try {
                int maxRetries = 1;
                if(filesServers.size() == 0 ||
                        (filesServers.size() == 1 && filesServers.peek() == toIgnore)){
                    maxRetries = ClientUtils.MAX_RETRIES;
                    Log.info("Attempting to send file to last files server");
                }
                polled.server.writeFile(fileId, data, "", maxRetries);
                polled.usedStorage += data.length;
                counter = polled;
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                counter = sendFile(data, fileId, toIgnore);
            }
        }
        filesServers.add(polled);
        return counter;
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
}
