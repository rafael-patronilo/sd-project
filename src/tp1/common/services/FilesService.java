package tp1.common.services;

import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.UnexpectedErrorException;

public interface FilesService {
    String NAME = "files";

    void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException;

    void deleteFile(String fileId, String token) throws InvalidFileLocationException;

    byte[] getFile(String fileId, String token) throws UnexpectedErrorException, InvalidFileLocationException;
}
