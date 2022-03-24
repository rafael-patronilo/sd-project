package tp1.server.resources;

import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;

import java.util.List;

public class DirectoryResource implements RestDirectory {

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
        return null;
    }

    @Override
    public void deleteFile(String filename, String userId, String password) {
        
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) {
        return new byte[0];
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        return null;
    }
}
