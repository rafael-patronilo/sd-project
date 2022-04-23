package tp1.common.clients;

import tp1.api.User;
import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidUserIdException;
import tp1.common.exceptions.RequestTimeoutException;

public interface UsersServerClient {
    /**
     * Obtains the information on the user identified by name.
     *
     * @param userId the userId of the user
     * @param password password of the user
     * @return the user object, if the userId exists and password matches the existing
     *         password
     * @throws InvalidUserIdException if no user exists with the provided userId
     * @throws IncorrectPasswordException if the password is incorrect
     * @throws RequestTimeoutException if the response takes too long to arrive.
     */
    User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException, RequestTimeoutException;

    /**
     * Checks if there's a user with the given id
     *
     * @param userId the userId of the user
     * @return true if there is a user with the given id, false otherwise
     * @throws RequestTimeoutException if the response takes too long to arrive.
     */
    boolean hasUser(String userId) throws RequestTimeoutException;
}
