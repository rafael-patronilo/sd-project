package sd2122.aula2.clients;

import java.io.IOException;

import sd2122.aula2.api.User;

public class UpdateUserClient {

	public static void main(String[] args) throws IOException {
		
		if( args.length != 6) {
			System.err.println( "Use: java sd2122.aula2.clients.UpdateUserClient url userId oldpwd fullName email password");
			return;
		}
		
		String serverUrl = args[0];
		String userId = args[1];
		String oldpwd = args[2];
		String fullName = args[3];
		String email = args[4];
		String password = args[5];
		
		User u = new User( userId, fullName, email, password);
		
		System.out.println("Sending request to server.");
		
		//TODO complete this client code
	}
	
}
