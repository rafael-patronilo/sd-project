package tp1.server.rest.resources;

import java.util.*;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.common.services.UsersService;

import static tp1.server.rest.RestUtils.*;

@Singleton
public class RestUsersResource implements RestUsers {
	private static Logger Log = Logger.getLogger(RestUsersResource.class.getName());
	private UsersService base = new UsersService();
		
	@Override
	public String createUser(User user) {
		return handleExceptions(()->base.createUser(user), Log);
	}


	@Override
	public User getUser(String userId, String password) {
		return handleExceptions(()->base.getUser(userId, password), Log);
	}


	@Override
	public User updateUser(String userId, String password, User user) {
		return handleExceptions(()->base.updateUser(userId, password, user), Log);
	}


	@Override
	public User deleteUser(String userId, String password) {
		return handleExceptions(()->base.deleteUser(userId, password), Log);
	}


	@Override
	public List<User> searchUsers(String pattern) {
		return handleExceptions(()->base.searchUsers(pattern), Log);
	}
}
