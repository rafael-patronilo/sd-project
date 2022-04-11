package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;


@Singleton
public class DirectoryResource implements RestDirectory {
    public final static String FILE_SERVERS_PROPERTY = "fileServers";
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

    Map<FileLocation, String> locationToId = new HashMap<>();
    Map<FileLocation, FileInfo> locationToInfo = new HashMap<>();
    Map<String, FilesServerCounter> idToServer = new HashMap<>();
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
    public FileInfo writeFile(String filename, byte[] data, String userId, String password){
        Log.info("writeFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileLocation location = new FileLocation(userId, filename);
        String fileId = locationToId.get(location);
        FileInfo info;
        if(fileId == null) {
            info = new FileInfo(userId, filename, null, new HashSet<>());
            fileId = String.valueOf(lastFileId++);
            Log.info(String.format("mapped %s/%s to file id %s", userId, filename, fileId));
            FilesServerCounter counter = filesServers.poll();
            if(counter==null) {
                Log.severe("write file request before file server discovered");
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
            locationToInfo.put(location, info);
            locationToId.put(location, fileId);
            try {
                counter.server.writeFile(fileId, data, "");
                info.setFileURL(String.format("%s/%s/%s", counter.server.getUri(), RestFiles.PATH, fileId));
                idToServer.put(fileId, counter);
                counter.fileCount++;
                filesServers.add(counter);
            } catch (RequestTimeoutException e) {
                Log.severe("timed out");
                //TODO handle timeouts
            }

        } else{
            info = locationToInfo.get(location);
            FilesServerProxy fileServer = idToServer.get(fileId).server;
            try{
                fileServer.writeFile(fileId, data, "");
                info.setFileURL(String.format("%s/%s/%s", fileServer.getUri(), RestFiles.PATH, fileId));
            } catch (RequestTimeoutException e){
                Log.severe("timed out");
                //TODO handle timeouts
            }
        }

        return info;
    }

    @Override
    public void deleteFile(String filename, String userId, String password) {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileLocation location = new FileLocation(userId, filename);
        if(locationToInfo.remove(location) == null){
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        String fileId = locationToId.remove(location);
        FilesServerCounter filesCounter = idToServer.remove(fileId);
        filesCounter.server.tryDeleteFile(fileId, "");
        filesServers.remove(filesCounter);
        filesCounter.fileCount--;
        filesServers.add(filesCounter);
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("shareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileInfo info = locationToInfo.get(new FileLocation(userId, filename));
        if(info == null) {
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        validatePassword(userId, password);
        if(!userId.equals(userIdShare))
            info.getSharedWith().add(userIdShare);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("unshareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileInfo info = locationToInfo.get(new FileLocation(userId, filename));
        if(info == null) {
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        validatePassword(userId, password);
        info.getSharedWith().remove(userIdShare);
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
        FileLocation location = new FileLocation(userId, filename);
        FileInfo info = locationToInfo.get(location);
        if(info == null) {
            Log.info("throw NOT FOUND: user doesn't have such file");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        if(!info.getSharedWith().contains(accUserId) && !isOwner) {
            Log.info("throw FORBIDDEN: user doesn't have access to file");
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        String fileId = locationToId.get(location);
        FilesServerProxy files = idToServer.get(fileId).server;
        files.redirectToGetFile(fileId, "");
        Log.severe("unreachable");
        throw new WebApplicationException(Status.BAD_REQUEST);
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        Log.info("lsFile : userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        List<FileInfo> returning = new ArrayList<>();
        for(FileInfo info : locationToInfo.values()) {
            if(info.getOwner().equals(userId) || info.getSharedWith().contains(userId)) {
                try {
                    if(usersServer.hasUser(info.getOwner())) {
                        returning.add(info);
                    }
                } catch (RequestTimeoutException e) {
                    //TODO handle timeout
                }
            }
        }
        return returning;
    }

    public List<String> deletedFiles(String serverUri, List<String> fileIds){
        List<String> returning = new ArrayList<>();
        for(String fileId : fileIds){
            FilesServerProxy fileServer = idToServer.get(fileId).server;
            if(fileServer == null || !serverUri.equals(fileServer.getUri()))
                returning.add(fileId);
        }
        return returning;
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
