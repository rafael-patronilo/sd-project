package tp1.common.services;

import tp1.api.FileInfo;
import tp1.common.WithHeader;
import tp1.common.exceptions.*;

import java.util.List;

public interface DirectoryService {
    String LAST_FILE_OP_HEADER = "X-DFS-Last-File-Op";
    String NAME = "directory";

    WithHeader<FileInfo> writeFile(String filename, byte[] data, String userId, String password) throws UnexpectedErrorException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    WithHeader<Object> deleteFile(String filename, String userId, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void shareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    void unshareFile(String filename, String userId, String userIdShare, String password) throws InvalidFileLocationException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    byte[] getFile(String filename, String userId, String accUserId, String password, boolean tryRedirect) throws InvalidFileLocationException, NoAccessException, RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    List<FileInfo> lsFile(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException, InvalidUserIdException;

    WithHeader<Object> deleteDirectory(String userId, String password) throws RequestTimeoutException, IncorrectPasswordException;
}
