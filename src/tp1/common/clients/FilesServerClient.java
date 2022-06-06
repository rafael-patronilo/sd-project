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
     *
     * @return the URI
     */
    String getURI();

    /**
     * Write a file. If the file exists, overwrites the contents.
     *
     * @param fileId - unique id of the file.
     * @throws RequestTimeoutException if the response takes too long to arrive.
     */
    void writeFile(String fileId, byte[] data, int maxRetries) throws RequestTimeoutException;

    /**
     * Tries to delete an existing file but doesn't wait for the answer.
     *
     * @param fileId - unique id of the file.
     */
    void deleteFileAsync(String fileId);

    /**
     * Sends a redirect to the file server. Not always implemented
     *
     * @param fileId - unique id of the file.
     */
    void redirectToGetFile(String fileId);

    /**
     * Sends a redirect to the file server. Not always implemented
     *
     * @param fileId - unique id of the file.
     *               project this will not be used).
     */
    void redirectToGetFile(String fileId, long version);

    /**
     * Gets the file specified by fileId
     *
     * @param fileId - unique id of the file.
     *               project this will not be used).
     * @return the file's contents
     * @throws RequestTimeoutException      if the response takes too long to arrive.
     * @throws InvalidFileLocationException if the server has no file with the given id.
     */
    byte[] getFile(String fileId) throws RequestTimeoutException, InvalidFileLocationException;

    /**
     * Gets the file specified by fileId
     *
     * @param fileId - unique id of the file.
     *               project this will not be used).
     * @return the file's contents
     * @throws RequestTimeoutException      if the response takes too long to arrive.
     * @throws InvalidFileLocationException if the server has no file with the given id.
     */
    byte[] getFile(String fileId, long version) throws RequestTimeoutException, InvalidFileLocationException;
}

