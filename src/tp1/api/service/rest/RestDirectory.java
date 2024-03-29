package tp1.api.service.rest;

import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tp1.api.FileInfo;
import tp1.common.services.DirectoryService;
import tp1.kafka.operations.Operation;

@Path(RestDirectory.PATH)
public interface RestDirectory {

	static final String PATH="/dir";

	/**
	 * Write a new version of a file. If the file exists, its contents are overwritten.
	 * Only the owner (userId) can write the file.
	 * 
	 * A file resource will has the full path "userId/filename".
	 * 
	 * @param filename - name of the file.
	 * @param data - contents of the file.
	 * @param userId - id of the user.
	 * @param password - the password of the user.
	 * 
	 * @return 200 if success + FileInfo representing the file.
	 *		   404 if the userId does not exist.
	 *         403 if the password is incorrect.
	 * 		   400 otherwise.
	 */
	@POST
	@Path("/{userId}/{filename}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	FileInfo writeFile(@PathParam("filename") String filename, byte []data, 
			@PathParam("userId") String userId, @QueryParam("password") String password);

	/**
	 * Delete an existing file ("userId/filename"). 
	 * Only the owner (userId) can delete the file.
	 * 
	 * @param filename - name of the file.
	 * @param userId - id of the user.
	 * @param password - the password of the user.
	 * 
	 * @return 204 if success; 
	 *		   404 if the userId or filename does not exist.
	 *         403 if the password is incorrect.
	 * 		   400 otherwise.
	 */
	@DELETE
	@Path("/{userId}/{filename}")
	void deleteFile(@PathParam("filename") String filename, 
			@PathParam("userId") String userId, @QueryParam("password") String password);

	/**
	 * Share the file "userId/filename" with another user. 
	 * Only the owner (userId) can share the file.
	 * 
	 * The operation succeeds if and only if the userId, userIdShare and userId/filename
	 * exist and password is correct (note: if userIdShare already has access to the file,
	 * the operation still succeeds).
	 * 
	 * @param filename - name of the file.
	 * @param userId - id of the user.
	 * @param userIdShare - id of the user to share the file with.
	 * @param password - the password of the user.
	 * 
	 * @return 204 if success; 
	 *		   404 if the userId or userIdShare or filename does not exist.
	 *         403 if the password is incorrect.
	 * 		   400 otherwise.
	 */
	@POST
	@Path("/{userId}/{filename}/share/{userIdShare}")
	void shareFile(@PathParam("filename") String filename, @PathParam("userId") String userId, 
			@PathParam("userIdShare") String userIdShare, @QueryParam("password") String password);

	/**
	 * Unshare the file "userId/filename" with another user. 
	 * Only the owner (userId) can unshare the file.
	 * 
	 * The operation succeeds if and only if the userId, userIdShare and userId/filename
	 * exist and password is correct (note: if userIdShare does not have access to the file,
	 * the operation still succeeds).
	 * 
	 * @param filename - name of the file.
	 * @param userId - id of the user.
	 * @param userIdShare - id of the user to unshare the file with.
	 * @param password - the password of the user.
	 * 
	 * @return 204 if success; 
	 *		   404 if the userId or userIdShare or filename does not exist.
	 *         403 if the password is incorrect.
	 * 		   400 otherwise.
	 */
	@DELETE
	@Path("/{userId}/{filename}/share/{userIdShare}")
	void unshareFile(@PathParam("filename") String filename, @PathParam("userId") String userId, 
			@PathParam("userIdShare") String userIdShare, @QueryParam("password") String password);

	/**
	 * Get the contents of the file "userId/filename". 
	 * Who can read a file: the owner and the users with whom the file has been shared.
	 * 
	 * This operation should be implemented using HTTP redirect on success.
	 * 
	 * @param filename - name of the file.
	 * @param userId - id of the user.
	 * @param accUserId - id of the user executing the operation.
	 * @param password - the password of accUserId.
	 * 
	 * @return 200 if success + contents (through redirect to the File server); 
	 *		   404 if the userId or filename or accUserId does not exist.
	 *         403 if the password is incorrect or the user cannot access the file.
	 * 		   400 otherwise.
	 */
	@GET
	@Path("/{userId}/{filename}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	byte[] getFile(@PathParam("filename") String filename, @PathParam("userId") String userId, 
			@QueryParam("accUserId") String accUserId, @QueryParam("password") String password,
				   @HeaderParam(DirectoryService.VERSION_HEADER) @DefaultValue("-1") long version);

	/**
	 * List the files a given user ("userId") has access to - this includes both its own files
	 * and the files shared with her. 
	 *  
	 * @param userId - id of the user.
	 * @param password - the password of the user.
	 * 
	 * @return 200 if success + list of FileInfo; 
	 *		   404 if the userId does not exist.
	 *         403 if the password is incorrect.
	 * 		   400 otherwise.
	 */
	@GET
	@Path("/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	List<FileInfo> lsFile(@PathParam("userId") String userId, 
			@QueryParam("password") String password);

	/**
	 * Deletes all files from a given user. If the user doesn't exist, the operation is successful anyway
	 * @param userId the user whose files are to be deleted
	 * @param password the user's password
	 * @return 204 if success;
	 * 		   403 if the password is incorrect
	 * 		   400 otherwise
	 */
	@DELETE
	@Path("/{userId}")
	void deleteDirectory(@PathParam("userId") String userId, @QueryParam("password") String password,
						 @QueryParam("token") String token);
}
