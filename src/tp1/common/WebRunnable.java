package tp1.common;

import tp1.common.exceptions.*;

public interface WebRunnable {
    void invoke() throws
            IncorrectPasswordException, InvalidFileLocationException, InvalidUserIdException,
            NoAccessException, RequestTimeoutException, UnexpectedErrorException,
            InvalidArgumentException, ConflicitingUsersException;
}
