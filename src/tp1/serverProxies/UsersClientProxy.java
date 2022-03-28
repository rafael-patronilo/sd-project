package tp1.serverProxies;

import tp1.api.User;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;

public interface UsersClientProxy {
    /**
     * Obtains the information on the user identified by name.
     *
     * @param userId the userId of the user
     * @param password password of the user
     * @return the user object, if the userId exists and password matches the existing
     *         password
     * @throws InvalidUserIdException if no user exists with the provided userId
     * @throws IncorrectPasswordException if the password is incorrect
     */
    User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException;

    /**
     * Checks if there's a user with the given id
     *
     * @param userId the userId of the user
     * @return true if there is a user with the given id, false otherwise
     */
    boolean hasUser(String userId);
}
