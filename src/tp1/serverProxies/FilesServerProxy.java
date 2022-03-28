package tp1.serverProxies;

import tp1.serverProxies.exceptions.RequestTimeoutException;

public interface FilesServerProxy {

    /**
     * Gets this server's uri
     */
    String getUri();

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
    void writeFile(String fileId, byte[] data, String token) throws RequestTimeoutException;

    /**
     * Tries to delete an existing file but doesn't wait for the answer.
     *
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     */
    void tryDeleteFile(String fileId, String token);

    /**
     * Sends a redirect to the file server.
     *
     * @param fileId - unique id of the file.
     * @param token - token for accessing the file server (in the first
     * project this will not be used).
     *
     */
    void redirectToGetFile(String fileId, String token);

}
