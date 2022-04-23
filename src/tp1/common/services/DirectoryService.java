package tp1.common.services;

import tp1.api.FileInfo;
import tp1.api.User;
import tp1.client.ClientUtils;
import tp1.client.rest.RestFilesClient;
import tp1.client.rest.RestUsersClient;
import tp1.client.soap.SoapFilesClient;
import tp1.client.soap.SoapUsersClient;
import tp1.common.clients.FilesServerClient;
import tp1.common.clients.UsersServerClient;
import tp1.common.exceptions.*;
import tp1.server.MulticastServiceDiscovery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DirectoryService {
    public static final String NAME = "directory";
    private static final Logger Log = Logger.getLogger(DirectoryService.class.getName());
    private UsersServerClient usersServer = null;
    private final PriorityQueue<FileServerMonitor> filesServers = new PriorityQueue<>();
    private final Map<String, Map<String, FileReference>> directories = new HashMap<>();
    private AtomicLong lastFileId = new AtomicLong();

    private static class FileServerMonitor implements Comparable<FileServerMonitor> {
        FilesServerClient server;
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
    private static class FileReference{
        String fileId;
        FileServerMonitor server;
        FileInfo info;

        int size;

        public FileReference(String fileId, FileServerMonitor server, FileInfo info, int size) {
            this.fileId = fileId;
            this.server = server;
            this.info = info;
            this.size = size;
        }

    }

    public DirectoryService(){
        MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
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
        for(String uri : discovery.discoveredServices(FilesService.NAME)){
            filesListener.accept(uri);
        }
        discovery.listenForServices(FilesService.NAME, filesListener);
        Set<String> users = discovery.discoveredServices(UsersService.NAME);
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
        if(users.isEmpty()){
            discovery.listenForServices(UsersService.NAME, userListener);
        } else {
            userListener.accept(users.iterator().next());
        }
    }

    public FileInfo writeFile(String filename, byte[] data, String userId, String password) throws UnexpectedErrorException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        Log.info("writeFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        Map<String, FileReference> directory = getDirectory(userId);
        FileReference reference = directory.get(filename);
        if (reference == null) {
            FileInfo info = new FileInfo(userId, filename, null, new HashSet<>());
            String fileId = String.valueOf(lastFileId.getAndIncrement());
            Log.info(String.format("mapped %s/%s to file id %s", userId, filename, fileId));
            FileServerMonitor counter = sendFile(data, fileId);
            reference = new FileReference(fileId, counter, info, data.length);
            reference.info.setFileURL(counter.server.getFileDirectUrl(fileId));
            directory.put(filename, reference);
        } else {
            FileServerMonitor counter = reference.server;
            FilesServerClient originalServer = counter.server;
            try {
                int maxRetries = filesServers.size() == 1 ? ClientUtils.MAX_RETRIES : 1;
                originalServer.writeFile(reference.fileId, data, "", maxRetries);
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                reference.server = sendFile(data, reference.fileId, counter);
                reference.info.setFileURL(counter.server.getFileDirectUrl(reference.fileId));

                //Try to clean the file from the old server
                synchronized (filesServers) {
                    filesServers.remove(counter);
                    counter.usedStorage -= reference.size;
                    filesServers.add(counter);
                }
                reference.size = data.length;
                originalServer.deleteFileAsync(reference.fileId, "");
            }
        }

        return reference.info;
    }

    public void deleteFile(String filename, String userId, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileReference removed = getDirectory(userId).remove(filename);
        if(removed == null){
            Log.info("throw InvalidFilenameException: user doesn't have such file");
            throw new InvalidFileLocationException();
        }
        removed.server.server.deleteFileAsync(removed.fileId, "");
        synchronized (filesServers) {
            filesServers.remove(removed.server);
            removed.server.usedStorage -= removed.size;
            filesServers.add(removed.server);
        }
    }

    public void shareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
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

    public void unshareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
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

    public byte[] getFile(String filename, String userId, String accUserId, String password, boolean tryRedirect) throws InvalidFileLocationException, NoAccessException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        Log.info("getFile : filename = " + filename + "; userId = " + userId + "; accUserId = "
                + accUserId + "; password = " + password);
        validatePassword(accUserId, password);
        boolean isOwner = userId.equals(accUserId);
        if(!isOwner){
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
        if(tryRedirect) {
            Log.info("Attempting to redirect to files server");
            files.redirectToGetFile(reference.fileId, "");
            Log.info("Failed to redirect");
        }
        return reference.server.server.getFile(reference.fileId, "");
    }

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

    public void deleteDirectory(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException {
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
    }

    private void validatePassword(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException {
        usersServer.getUser(userId, password);
    }

    private FileServerMonitor sendFile(byte[] data, String fileId) throws UnexpectedErrorException {
        return sendFile(data, fileId, null);
    }

    private synchronized FileServerMonitor sendFile(byte[] data, String fileId, FileServerMonitor toIgnore) throws UnexpectedErrorException {
        FileServerMonitor counter;
        FileServerMonitor polled = filesServers.poll();
        if(polled == null){
            Log.severe("No file servers or all file servers timed out");
            throw new UnexpectedErrorException();
        }

        if(polled == toIgnore){
            counter = sendFile(data, fileId, toIgnore);
        } else {
            try {
                int maxRetries = 1;
                if(filesServers.size() == 0 ||
                        (filesServers.size() == 1 && filesServers.peek() == toIgnore)){
                    maxRetries = ClientUtils.MAX_RETRIES;
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
