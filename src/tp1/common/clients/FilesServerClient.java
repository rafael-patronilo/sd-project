package tp1.common.clients;

import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.RequestTimeoutException;

/**
 * Minimal client interface with files operations required by some services
 */
public interface FilesServerClient {

    /**
     * Gets the file's direct url
     */
    String getFileDirectUrl(String fileId);

    /**
     * This server's URI
     * @return the URI
     */
    String getURI();

    /**
     * Write a file. If the file exists, overwrites the contents.
     *
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     *
     * @throws RequestTimeoutException if the response takes too long to arrive.
     *
     */
    void writeFile(String fileId, byte[] data, String token, int maxRetries) throws RequestTimeoutException;

    /**
     * Tries to delete an existing file but doesn't wait for the answer.
     *
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     */
    void deleteFileAsync(String fileId, String token);

    /**
     * Sends a redirect to the file server. Not always implemented
     *
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     *
     */
    void redirectToGetFile(String fileId, String token);

    /**
     * Gets the file specified by fileId
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     * @return the file's contents
     *
     * @throws RequestTimeoutException if the response takes too long to arrive.
     * @throws InvalidFileLocationException if the server has no file with the given id.
     */
    byte[] getFile(String fileId, String token) throws RequestTimeoutException, InvalidFileLocationException;

}
