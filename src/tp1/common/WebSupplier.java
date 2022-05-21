package tp1.common;

import tp1.common.exceptions.*;

/**
 * Functional interface for an operation with a return that may throw an exception
 * @param <T> the return type of the operation
 */
public interface WebSupplier<T> {
    T invoke() throws
            IncorrectPasswordException, InvalidFileLocationException, InvalidUserIdException,
            NoAccessException, RequestTimeoutException, UnexpectedErrorException,
            InvalidArgumentException, ConflicitingUsersException;
}
