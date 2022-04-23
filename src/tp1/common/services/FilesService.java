package tp1.common.services;

import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.UnexpectedErrorException;

import java.io.*;
import java.util.logging.Logger;

public class FilesService {
    public final static String NAME = "files";
    private static final String STORAGE_PATH = "./%s";
    private static Logger Log = Logger.getLogger(FilesService.class.getName());


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

    public void deleteFile(String fileId, String token) throws InvalidFileLocationException {
        Log.info("deleteFile : " + fileId);
        File file = new File(pathTo(fileId));
        if(!file.delete()){
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        }
    }

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

    private static String pathTo(String fileId){
        return String.format(STORAGE_PATH, fileId);
    }
}
