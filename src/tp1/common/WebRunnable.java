package tp1.common;

import tp1.common.exceptions.*;

/**
 * Functional interface for an operation with no return that may throw an exception
 */
public interface WebRunnable {
    void invoke() throws
            IncorrectPasswordException, InvalidFileLocationException, InvalidUserIdException,
            NoAccessException, RequestTimeoutException, UnexpectedErrorException,
            InvalidArgumentException, ConflicitingUsersException, InvalidTokenException;
}
