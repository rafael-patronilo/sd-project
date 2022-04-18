package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;
import tp1.api.service.rest.RestFiles;
import tp1.server.RESTFilesServer;
import tp1.server.MulticastServiceDiscovery;
import tp1.server.RESTUsersServer;
import tp1.serverProxies.FilesServerProxy;
import tp1.serverProxies.RestFilesServer;
import tp1.serverProxies.RestUsersServer;
import tp1.serverProxies.UsersServerProxy;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

import javax.annotation.processing.Filer;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;


@Singleton
public class DirectoryResource implements RestDirectory {
    private UsersServerProxy usersServer = null;
    private PriorityQueue<FilesServerCounter> filesServers = new PriorityQueue<>();

    private record FileLocation(String userId, String filename) {}
    private class FilesServerCounter implements Comparable<FilesServerCounter> {
        FilesServerProxy server;
        int fileCount;
        FilesServerCounter(FilesServerProxy server){
            this.server = server;
            this.fileCount = 0;
        }

        @Override
        public int compareTo(FilesServerCounter other) {
            return Integer.compare(this.fileCount, other.fileCount);
        }
    }
    private class FileReference{
        String fileId;
        FilesServerCounter server;
        FileInfo info;
        public FileReference(String fileId, FilesServerCounter server, FileInfo info) {
            this.fileId = fileId;
            this.server = server;
            this.info = info;
        }
    }

    Map<String, Map<String, FileReference>> directories = new HashMap<>();
    long lastFileId = 0;

    private static Logger Log = Logger.getLogger(DirectoryResource.class.getName());

    public DirectoryResource(){
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
        Consumer<String> filesListener = (String uri) ->{
            try {
                FilesServerProxy proxy = null;
                if(uri.endsWith("rest"))
                    proxy = new RestFilesServer(client.target(new URI(uri)), uri);
                filesServers.add(new FilesServerCounter(proxy));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        };
        for(String uri : discovery.discoveredServices(RESTFilesServer.SERVICE)){
            filesListener.accept(uri);
        }
        discovery.listenForServices(RESTFilesServer.SERVICE, filesListener);
        Set<String> users = discovery.discoveredServices(RESTUsersServer.SERVICE);
        Consumer<String> userListener = (String uri)->{
            if(usersServer == null){
                try {
                    usersServer = new RestUsersServer(client.target(new URI(uri)));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
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
        FileLocation location = new FileLocation(userId, filename);
        Map<String, FileReference> directory = getDirectory(userId);
        FileReference reference = directory.get(filename);
        if (reference == null) {
            FileInfo info = new FileInfo(userId, filename, null, new HashSet<>());
            String fileId = String.valueOf(lastFileId++);
            Log.info(String.format("mapped %s/%s to file id %s", userId, filename, fileId));
            FilesServerCounter counter = sendFile(data, fileId);
            reference = new FileReference(fileId, counter, info);
            reference.info.setFileURL(
                    String.format("%s/%s/%s", counter.server.getUri(), RestFiles.PATH, reference.fileId));
            directory.put(filename, reference);
        } else {
            FilesServerCounter counter = reference.server;
            FilesServerProxy originalServer = counter.server;
            try {
                originalServer.writeFile(reference.fileId, data, "");
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                FilesServerCounter newServer = sendFile(data, reference.fileId, counter);
                reference.server = newServer;
                reference.info.setFileURL(
                        String.format("%s/%s/%s", newServer.server.getUri(), RestFiles.PATH, reference.fileId));

                //Try to clean the file from the old server
                filesServers.remove(counter);
                counter.fileCount--;
                filesServers.add(counter);
                originalServer.tryDeleteFile(reference.fileId, "");
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
        removed.server.server.tryDeleteFile(removed.fileId, "");
        filesServers.remove(removed.server);
        removed.server.fileCount--;
        filesServers.add(removed.server);
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
        FileReference reference = getDirectory(userId).get(filename);
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
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        Log.info("lsFile : userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        List<FileInfo> returning = new ArrayList<>();
        for (Map<String, FileReference> directory : directories.values()){
            for (FileReference reference : directory.values()) {
                FileInfo info = reference.info;
                if (info.getOwner().equals(userId) || info.getSharedWith().contains(userId)) {
                    returning.add(info);
                }
            }
        }
        return returning;
    }

    @Override
    public void deleteDirectory(String userId, String password) {
        validatePassword(userId, password);
        Map<String, FileReference> directory = getDirectory(userId);
        for (FileReference reference : directory.values()) {
            reference.server.server.tryDeleteFile(reference.fileId, "");
        }
        directory.clear();
    }

    public List<String> deletedFiles(String serverUri, List<String> fileIds){
        /* TODO decide how to do this
        List<String> returning = new ArrayList<>();
        for(String fileId : fileIds){
            FilesServerProxy fileServer = idToServer.get(fileId).server;
            if(fileServer == null || !serverUri.equals(fileServer.getUri()))
                returning.add(fileId);
        }
        return returning;
        */
        throw new RuntimeException("Not implemented");
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

    private FilesServerCounter sendFile(byte[] data, String fileId){
        return sendFile(data, fileId, null);
    }

    private FilesServerCounter sendFile(byte[] data, String fileId, FilesServerCounter toIgnore){
        FilesServerCounter counter;
        FilesServerCounter polled = filesServers.poll();
        if(polled == null){
            Log.severe("No file servers or all file servers timed out");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if(polled == toIgnore){
            counter = sendFile(data, fileId, toIgnore);
        } else {
            try {
                polled.server.writeFile(fileId, data, "");
                polled.fileCount++;
                counter = polled;
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                counter = sendFile(data, fileId, toIgnore);
            }
        }
        filesServers.add(polled);
        return counter;
    }

    private Map<String, FileReference> getDirectory(String userId){
        return directories.computeIfAbsent(userId, k -> new HashMap<>());
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
