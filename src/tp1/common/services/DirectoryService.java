package tp1.common.services;

import tp1.api.FileInfo;
import tp1.common.exceptions.*;

import java.util.List;

public interface DirectoryService {
    String VERSION_HEADER = "X-DFS-Version";
    String NAME = "directory";

    FileInfo writeFile(String filename, byte[] data, String userId, String password) throws UnexpectedErrorException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void deleteFile(String filename, String userId, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void shareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void unshareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    byte[] getFile(String filename, String userId, String accUserId, String password, boolean tryRedirect, long version) throws InvalidFileLocationException, NoAccessException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    List<FileInfo> lsFile(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void deleteDirectory(String userId, String password, String token) throws RequestTimeoutException, IncorrectPasswordException, InvalidTokenException;
}
