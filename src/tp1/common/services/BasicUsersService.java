package tp1.common.services;

import tp1.api.User;
import tp1.client.rest.RestDirClient;
import tp1.client.soap.SoapDirClient;
import tp1.common.clients.DirServerClient;
import tp1.common.exceptions.ConflicitingUsersException;
import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidArgumentException;
import tp1.common.exceptions.InvalidUserIdException;
import tp1.server.MulticastServiceDiscovery;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Users services
 */
public class BasicUsersService implements UsersService {

    private final Map<String,User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(BasicUsersService.class.getName());
	private DirServerClient directoryServer;
	
	public BasicUsersService() {
		// listener for directory services
		Consumer<String> directoryListener = (uri) ->{
			if (uri.endsWith("rest")) {
				directoryServer = new RestDirClient(uri);
			} else if (uri.endsWith("soap")){
				directoryServer = new SoapDirClient(uri);
			}
		};
		MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();

		// sets the directory server if already discovered, otherwise adds the listener
		Set<String> discovered = discovery.discoveredServices(DirectoryService.NAME);
		if(discovered.isEmpty()){
			discovery.listenForServices(DirectoryService.NAME, directoryListener);
		} else{
			directoryListener.accept(discovered.iterator().next());
		}
	}

	@Override
    public String createUser(User user) throws InvalidArgumentException, ConflicitingUsersException {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			Log.info("throw InvalidArgument: User object invalid.");
			throw new InvalidArgumentException();
		}
		synchronized (users) {
			// Check if userId already exists
			if (users.containsKey(user.getUserId())) {
				Log.info("throw ConflicitingUsers: User already exists.");
				throw new ConflicitingUsersException();
			}

			//Add the user to the map of users
			users.put(user.getUserId(), user);
		}
		return user.getUserId();
	}


	@Override
    public User getUser(String userId, String password) throws IncorrectPasswordException, InvalidUserIdException {
		Log.info("getUser : user = " + userId + "; pwd = " + password);
		return validateUser(userId, password);
	}


	@Override
    public User updateUser(String userId, String password, User user) throws IncorrectPasswordException, InvalidUserIdException {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		User oldUser = validateUser(userId, password);

		// update the non null fields
		if(user.getFullName() != null)
			oldUser.setFullName(user.getFullName());
		if(user.getEmail() != null)
			oldUser.setEmail(user.getEmail());
		if(user.getPassword() != null)
			oldUser.setPassword(user.getPassword());

		return oldUser;
	}

	@Override
    public User deleteUser(String userId, String password) throws IncorrectPasswordException, InvalidUserIdException {
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

	/**
	 * Validates that the user exists and the password is correct
	 * @param userId the user id to validate
	 * @param password the user's password
	 * @return the user object
	 * @throws InvalidUserIdException if there's no user for given id
	 * @throws IncorrectPasswordException if the password is incorrect
	 */
	private synchronized User validateUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException {
		User user = users.get(userId);

		// Check if user exists
		if( user == null ) {
			Log.info("throw InvalidUserId: User does not exist.");
			throw new InvalidUserIdException();
		}

		//Check if the password is correct
		if(!user.getPassword().equals(password)) {
			Log.info("throw IncorrectPassword: Password is incorrect.");
			throw new IncorrectPasswordException();
		}

		return user;
	}
}
