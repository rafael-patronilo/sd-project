package tp1.common.services;

import tp1.api.User;
import tp1.common.exceptions.ConflicitingUsersException;
import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidArgumentException;
import tp1.common.exceptions.InvalidUserIdException;

import java.util.List;

public interface UsersService {
    String NAME = "users";

    String createUser(User user) throws InvalidArgumentException, ConflicitingUsersException;

    User getUser(String userId, String password) throws IncorrectPasswordException, InvalidUserIdException;

    User updateUser(String userId, String password, User user) throws IncorrectPasswordException, InvalidUserIdException;

    User deleteUser(String userId, String password) throws IncorrectPasswordException, InvalidUserIdException;

    List<User> searchUsers(String pattern);
}
