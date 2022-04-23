package tp1.common;

import tp1.common.exceptions.*;

public interface WebSupplier<T> {
    T invoke() throws
            IncorrectPasswordException, InvalidFileLocationException, InvalidUserIdException,
            NoAccessException, RequestTimeoutException, UnexpectedErrorException,
            InvalidArgumentException, ConflicitingUsersException;
}
