package tp1.common.services;

import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.InvalidTokenException;
import tp1.common.exceptions.UnexpectedErrorException;

public interface FilesService {
    public static final int NUMBER_OF_REPLICAS = 2;

    String NAME = "files";

    void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException, InvalidTokenException;

    void deleteFile(String fileId, String token) throws InvalidFileLocationException, InvalidTokenException;

    byte[] getFile(String fileId, String token, long version) throws UnexpectedErrorException,
            InvalidFileLocationException, InvalidTokenException;
}
