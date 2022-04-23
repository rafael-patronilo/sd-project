package tp1.server.resources;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.server.MulticastServiceDiscovery;
import tp1.server.RESTDirServer;
import tp1.serverProxies.DirServerProxy;
import tp1.serverProxies.RestDirClient;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String,User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());
	private DirServerProxy directoryServer;
	
	public UsersResource() {
		Consumer<String> directoryListener = (uri) ->{
			if (uri.endsWith("rest")) {
				directoryServer = new RestDirClient(uri);
			}
		};
		MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
		Set<String> discovered = discovery.discoveredServices(RESTDirServer.SERVICE);
		if(discovered.isEmpty()){
			discovery.listenForServices(RESTDirServer.SERVICE, directoryListener);
		} else{
			directoryListener.accept(discovered.iterator().next());
		}
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
		synchronized (users) {
			// Check if userId already exists
			if (users.containsKey(user.getUserId())) {
				Log.info("throw CONFLICT: User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}

			//Add the user to the map of users
			users.put(user.getUserId(), user);
		}
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
		User oldUser = validateUser(userId, password);

		if(user.getFullName() != null)
			oldUser.setFullName(user.getFullName());
		if(user.getEmail() != null)
			oldUser.setEmail(user.getEmail());
		if(user.getPassword() != null)
			oldUser.setPassword(user.getPassword());

		return oldUser;
	}


	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		User user;
		synchronized (users) {
			user = validateUser(userId, password);
			users.remove(userId);
		}
		directoryServer.deleteDirectoryAsync(userId, password);
		return user;
	}


	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);
		List<User> returning  = new ArrayList<>();
		synchronized (users) {
			for (User user : users.values()) {
				if (user.getFullName().toLowerCase().contains(pattern.toLowerCase())) {
					returning.add(user);
				}
			}
		}
		return returning;
	}

	@Override
	public boolean hasUser(String userId) {
		return users.containsKey(userId);
	}

	private synchronized User validateUser(String userId, String password) {
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
