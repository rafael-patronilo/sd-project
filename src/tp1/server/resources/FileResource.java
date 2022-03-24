package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.service.rest.RestFiles;

import java.io.*;
import java.util.logging.Logger;

@Singleton
public class FileResource implements RestFiles {
    private static String STORAGE_PATH = "./storage/%s";
    private static Logger Log = Logger.getLogger(FileResource.class.getName());

    @Override
    public void writeFile(String fileId, byte[] data, String token) {
        Log.info("writeFile : " + fileId);
        try{
            FileOutputStream out = new FileOutputStream(pathTo(fileId));
            out.write(data);
            out.close();
        } catch (IOException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public void deleteFile(String fileId, String token) {
        Log.info("deleteFile : " + fileId);
        File file = new File(pathTo(fileId));
        if(file.delete()){
            throw new WebApplicationException(Status.NO_CONTENT);
        }
        else{
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    @Override
    public byte[] getFile(String fileId, String token) {
        Log.info("getFile : " + fileId);
        byte[] data = null;
        try {
            FileInputStream in = new FileInputStream(pathTo(fileId));
            data = in.readAllBytes();
            in.close();
        } catch (FileNotFoundException e) {
            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (IOException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return data;
    }

    private static String pathTo(String fileId){
        return String.format(STORAGE_PATH, fileId);
    }
}
