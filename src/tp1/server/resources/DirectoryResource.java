package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;
import tp1.server.RESTFilesServer;
import tp1.server.MulticastServiceDiscovery;
import tp1.server.RESTUsersServer;
import tp1.serverProxies.FilesServerProxy;
import tp1.serverProxies.RestFilesClient;
import tp1.serverProxies.RestUsersClient;
import tp1.serverProxies.UsersServerProxy;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
@Singleton
public class DirectoryResource implements RestDirectory {
    private static final Logger Log = Logger.getLogger(DirectoryResource.class.getName());
    private UsersServerProxy usersServer = null;
    private final PriorityQueue<FileServerMonitor> filesServers = new PriorityQueue<>();
    private final Map<String, Map<String, FileReference>> directories = new HashMap<>();
    private AtomicLong lastFileId = new AtomicLong();

    private static class FileServerMonitor implements Comparable<FileServerMonitor> {
        FilesServerProxy server;
        int usedStorage;
        FileServerMonitor(FilesServerProxy server){
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

    public DirectoryResource(){
        MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
        Consumer<String> filesListener = (String uri) ->{
            FilesServerProxy proxy = null;
            if(uri.endsWith("rest")) {
                proxy = new RestFilesClient(uri);
            }
            filesServers.add(new FileServerMonitor(proxy));
        };
        for(String uri : discovery.discoveredServices(RESTFilesServer.SERVICE)){
            filesListener.accept(uri);
        }
        discovery.listenForServices(RESTFilesServer.SERVICE, filesListener);
        Set<String> users = discovery.discoveredServices(RESTUsersServer.SERVICE);
        Consumer<String> userListener = (String uri)->{
            if(usersServer == null){
                usersServer = new RestUsersClient(uri);
            }
        };
        if(users.isEmpty()){
            discovery.listenForServices(RESTUsersServer.SERVICE, userListener);
        } else {
            userListener.accept(users.iterator().next());
        }
    }

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
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
            FilesServerProxy originalServer = counter.server;
            try {
                originalServer.writeFile(reference.fileId, data, "");
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                FileServerMonitor newServer = sendFile(data, reference.fileId, counter);
                reference.server = newServer;
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

    @Override
    public void deleteFile(String filename, String userId, String password) {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileReference removed = getDirectory(userId).remove(filename);
        if(removed == null){
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        removed.server.server.deleteFileAsync(removed.fileId, "");
        synchronized (filesServers) {
            filesServers.remove(removed.server);
            removed.server.usedStorage -= removed.size;
            filesServers.add(removed.server);
        }
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("shareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileReference reference = getDirectory(userId).get(filename);
        if(reference == null) {
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        validatePassword(userId, password);
        if(!userId.equals(userIdShare))
            reference.info.getSharedWith().add(userIdShare);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("unshareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileReference reference = getDirectory(userId).get(filename);
        if(reference == null) {
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        validatePassword(userId, password);
        reference.info.getSharedWith().remove(userIdShare);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) {
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
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        if(!reference.info.getSharedWith().contains(accUserId) && !isOwner) {
            Log.info("throw FORBIDDEN: user doesn't have access to file");
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        FilesServerProxy files = reference.server.server;
        files.redirectToGetFile(reference.fileId, "");
        Log.severe("unreachable");
        throw new WebApplicationException(Status.BAD_REQUEST);
        // TODO Soap doesn't have redirect
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
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
    public void deleteDirectory(String userId, String password) {
        Log.info("deleteDirectory : userId = " + userId + "; password = " + password);
        try {
            usersServer.getUser(userId, password);
        } catch (InvalidUserIdException ignored) {
            Log.info("User does not exist; deleting anyway");
        } catch (IncorrectPasswordException e) {
            Log.info("throw FORBIDDEN: incorrect password");
            throw new WebApplicationException(Status.FORBIDDEN);
        } catch (RequestTimeoutException e){
            Log.info("throw BAD REQUEST: has user request timed out");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        Map<String, FileReference> directory = getDirectory(userId);
        for (FileReference reference : directory.values()) {
            reference.server.server.deleteFileAsync(reference.fileId, "");
        }
        directory.clear();
    }

    private void validatePassword(String userId, String password){
        try {
            usersServer.getUser(userId, password);
        } catch (InvalidUserIdException e) {
            Log.info("throw NOT FOUND: userId doesn't exist");
            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (IncorrectPasswordException e) {
            Log.info("throw FORBIDDEN: incorrect password");
            throw new WebApplicationException(Status.FORBIDDEN);
        } catch (RequestTimeoutException e){
            Log.info("throw BAD REQUEST: has user request timed out");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    private FileServerMonitor sendFile(byte[] data, String fileId){
        return sendFile(data, fileId, null);
    }

    private synchronized FileServerMonitor sendFile(byte[] data, String fileId, FileServerMonitor toIgnore){
        FileServerMonitor counter;
        FileServerMonitor polled = filesServers.poll();
        if(polled == null){
            Log.severe("No file servers or all file servers timed out");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if(polled == toIgnore){
            counter = sendFile(data, fileId, toIgnore);
        } else {
            try {
                polled.server.writeFile(fileId, data, "");
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

    private void validateUser(String userId){
            try {
                if(!usersServer.hasUser(userId)){
                    Log.info("throw NOT FOUND: userId doesn't exist");
                    throw new WebApplicationException(Status.NOT_FOUND);
                }
            } catch (RequestTimeoutException e){
                Log.info("throw BAD REQUEST: has user request timed out");
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
    }
}
