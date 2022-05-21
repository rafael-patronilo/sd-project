package tp1.common.services;

import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.UnexpectedErrorException;

import java.io.*;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Files services
 */
public class LocalFilesService implements FilesService {
    // the path where files will be stored
    private static final String STORAGE_PATH = "./%s";
    private static Logger Log = Logger.getLogger(LocalFilesService.class.getName());


    @Override
    public void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException {
        Log.info("writeFile : " + fileId);
        try{
            FileOutputStream out = new FileOutputStream(pathTo(fileId));
            out.write(data);
            out.close();
        } catch (IOException e) {
            Log.info(String.format("throw UnexpectedError: IO Exception (%s)", e.getMessage()));
            throw new UnexpectedErrorException();
        }
    }

    @Override
    public void deleteFile(String fileId, String token) throws InvalidFileLocationException {
        Log.info("deleteFile : " + fileId);
        File file = new File(pathTo(fileId));
        if(!file.delete()){
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        }
    }

    @Override
    public byte[] getFile(String fileId, String token) throws UnexpectedErrorException, InvalidFileLocationException {
        Log.info("getFile : " + fileId);
        byte[] data = null;
        try {
            FileInputStream in = new FileInputStream(pathTo(fileId));
            data = in.readAllBytes();
            in.close();
        } catch (FileNotFoundException e) {
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        } catch (IOException e) {
            Log.info(String.format("throw UnexpectedError: IO Exception (%s)", e.getMessage()));
            throw new UnexpectedErrorException();
        }
        return data;
    }

    /**
     * Builds the path to the file given its id
     *
     * @param fileId the file's id
     * @return the file's path
     */
    static String pathTo(String fileId) {
        return String.format(LocalFilesService.STORAGE_PATH, fileId);
    }

}
