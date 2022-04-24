package tp1.common.clients;


/**
 * Minimal client interface with directory operations required by some services
 */
public interface DirServerClient {
    /**
     * Deletes all files from a given user
     * @param userId the user whose files are to be deleted
     * @param password the user's password
     */
    void deleteDirectoryAsync(String userId, String password);

}
