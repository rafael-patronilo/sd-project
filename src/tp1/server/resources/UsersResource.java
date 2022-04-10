package tp1.server.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String,User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());
	
	public UsersResource() {
	}
		
	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			Log.info("throw BAD REQUEST: User object invalid.");
			throw new WebApplicationException( Status.BAD_REQUEST );
		}
		
		// Check if userId already exists
		if( users.containsKey(user.getUserId())) {
			Log.info("throw CONFLICT: User already exists.");
			throw new WebApplicationException( Status.CONFLICT );
		}

		//Add the user to the map of users
		users.put(user.getUserId(), user);
		return user.getUserId();
	}


	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);
		return validateUser(userId, password);
	}


	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		if(user.getUserId() != null && user.getUserId().equals(userId)){
			Log.info("throw BAD REQUEST: Invalid attempt to change user id");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		User oldUser = validateUser(userId, password);

		user.setUserId(userId);
		if(user.getFullName() == null)
			user.setFullName(oldUser.getFullName());
		if(user.getEmail() == null)
			user.setEmail(oldUser.getEmail());
		if(user.getPassword() == null)
			user.setPassword(oldUser.getPassword());

		users.put(userId, user);
		return user;
	}


	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		User user = validateUser(userId, password);
		users.remove(userId);
		return user;
	}


	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);
		List<User> returning  = new ArrayList<>();
		for(User user : users.values()){
			if(user.getFullName().toLowerCase().contains(pattern.toLowerCase())){
				returning.add(user);
			}
		}
		return returning;
	}

	@Override
	public boolean hasUser(String userId) {
		return users.containsKey(userId);
	}

	private User validateUser(String userId, String password) {
		User user = users.get(userId);

		// Check if user exists
		if( user == null ) {
			Log.info("throw NOT FOUND: User does not exist.");
			throw new WebApplicationException( Status.NOT_FOUND );
		}

		//Check if the password is correct
		if(!user.getPassword().equals(password)) {
			Log.info("throw FORBIDDEN: Password is incorrect.");
			throw new WebApplicationException( Status.FORBIDDEN );
		}

		return user;
	}
}
