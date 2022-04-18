package tp1.serverProxies;

import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

public interface DirServerProxy {
    /**
     * Deletes all files from a given user
     * @param userId the user whose files are to be deleted
     * @param password the user's password
     * @throws InvalidUserIdException if no user exists with the provided userId
     * @throws IncorrectPasswordException if the password is incorrect
     * @throws RequestTimeoutException if the response takes too long to arrive.
     */
    void deleteDirectory(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException, RequestTimeoutException;

}
