package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;

import java.util.*;
import java.util.logging.Logger;


@Singleton
public class DirectoryResource implements RestDirectory {
    private record FileLocation(String userId, String filename) {}
    Map<FileLocation, String> locationToId = new HashMap<>();
    Map<FileLocation, FileInfo> locationToInfo = new HashMap<>();
    Map<String, String> idToServer = new HashMap<>();
    long lastFileId = 0;

    private static Logger Log = Logger.getLogger(DirectoryResource.class.getName());

    public DirectoryResource(){

    }

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
        Log.info("writeFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileInfo info = new FileInfo(userId, filename, userId + "/" + filename, new HashSet<>());
        String fileId = String.valueOf(lastFileId++);
        //TODO send the file
        return info;
    }

    @Override
    public void deleteFile(String filename, String userId, String password) {
        Log.info("deleteFile : filename = " + filename + "; userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        FileLocation location =new FileLocation(filename, userId);
        locationToInfo.remove(location);
        String fileId = locationToId.remove(location);
        idToServer.remove(fileId);
        //TODO Send delete request
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("shareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileInfo info = locationToInfo.get(new FileLocation(userId, filename));
        if(info == null)
            throw new WebApplicationException(Status.NOT_FOUND);

        validatePassword(userId, password);
        info.getSharedWith().add(userIdShare);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        Log.info("unshareFile : filename = " + filename + "; userId = " + userId + "; userIdShare = "
                + userIdShare + "; password = " + password);
        validateUser(userIdShare);
        FileInfo info = locationToInfo.get(new FileLocation(userId, filename));
        if(info == null)
            throw new WebApplicationException(Status.NOT_FOUND);

        validatePassword(userId, password);
        info.getSharedWith().remove(userIdShare);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) {
        Log.info("getFile : filename = " + filename + "; userId = " + userId + "; accUserId = "
                + accUserId + "; password = " + password);
        validatePassword(accUserId, password);
        return new byte[0];
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        Log.info("lsFile : userId = " + userId + "; password = " + password);
        validatePassword(userId, password);
        List<FileInfo> returning = new ArrayList<>();
        for(FileInfo info : locationToInfo.values()) {
            if(info.getOwner().equals(userId) || info.getSharedWith().contains(userId))
                returning.add(info);
        }
        return returning;
    }

    public List<String> deletedFiles(String server, List<String> fileIds){
        List<String> returning = new ArrayList<>();
        for(String fileId : fileIds){
            String fileServer = idToServer.get(fileId);
            if(!server.equals(fileServer))
                returning.add(fileId);
        }
        return returning;
    }

    private void validatePassword(String userId, String password){
        //TODO implement this
    }

    private void validateUser(String userId){
        //TODO implement this
    }
}
