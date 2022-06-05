package tp1.api.service.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tp1.common.services.DirectoryService;
import tp1.kafka.operations.Operation;

import java.util.List;

@Path(RestFiles.PATH)
public interface RestFiles {

	static final String PATH="/files";
	/**
	 * Write a file. If the file exists, overwrites the contents.
	 * 
	 * @param fileId - unique id of the file. 
	 * @param token - token for accessing the file server (in the first 
	 * project this will not be used).
     *
	 * @return 204 if success.
	 *         403 if the token is invalid.
	 * 		   400 otherwise.
	 */
	@POST
	@Path("/{fileId}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	void writeFile(@PathParam("fileId") String fileId, byte[] data, 
			@QueryParam("token") @DefaultValue("") String token);

	/**
	 * Delete an existing file.
	 * 
	 * @param fileId - unique id of the file. 
	 * @param token - token for accessing the file server (in the first 
	 * project this will not be used).
	 * 
	 * @return 204 if success; 
	 *		   404 if the fileId does not exist.
	 *         403 if the token is invalid.
	 * 		   400 otherwise.
	 */
	@DELETE
	@Path("/{fileId}")
	void deleteFile(@PathParam("fileId") String fileId, 
			@QueryParam("token") @DefaultValue("") String token);

	/**
	 * Get the contents of the file. 
	 * 
	 * @param fileId - unique id of the file. 
	 * @param token - token for accessing the file server (in the first 
	 * project this will not be used).
	 * 
	 * @return 200 if success + contents (through redirect to the File server); 
	 *		   404 if the fileId does not exist.
	 *         403 if the token is invalid.
	 * 		   400 otherwise.
	 */
	@GET
	@Path("/{fileId}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	byte[] getFile(@PathParam("fileId") String fileId, 
			@QueryParam("token") @DefaultValue("") String token,
				   @HeaderParam(DirectoryService.VERSION_HEADER) @DefaultValue("-1") long version);

}
