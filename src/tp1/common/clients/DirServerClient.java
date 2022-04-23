package tp1.common.clients;

import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidUserIdException;
import tp1.common.exceptions.RequestTimeoutException;

public interface DirServerClient {
    /**
     * Deletes all files from a given user
     * @param userId the user whose files are to be deleted
     * @param password the user's password
     * @throws InvalidUserIdException if no user exists with the provided userId
     * @throws IncorrectPasswordException if the password is incorrect
     * @throws RequestTimeoutException if the response takes too long to arrive.
     */
    void deleteDirectoryAsync(String userId, String password);

}
