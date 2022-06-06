package tp1.common.services;

import tp1.client.rest.RestFilesClient;
import tp1.client.soap.SoapFilesClient;
import tp1.common.ServerUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.InvalidTokenException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.common.exceptions.UnexpectedErrorException;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.KafkaUtils;
import tp1.kafka.operations.*;
import tp1.kafka.sync.SyncPoint;
import tp1.server.rest.RESTFilesServer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation of server operations for Files services
 */
public class LocalFilesService extends BaseFilesService {
    // the path where files will be stored
    private static final String STORAGE_PATH = "./%s";
    private static Logger Log = Logger.getLogger(LocalFilesService.class.getName());
    private static final List<String> TOPICS = Arrays.asList(KafkaUtils.DIR_FILES_TOPIC);

    public LocalFilesService(){
        super(Log);
    }

    @Override
    protected void writeFile(String fileId, byte[] data) throws UnexpectedErrorException {
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
    protected void deleteFile(String fileId) throws InvalidFileLocationException {
        File file = new File(pathTo(fileId));
        if(!file.delete()){
            Log.info("throw InvalidFileLocation: file not found");
            throw new InvalidFileLocationException();
        }
    }

    @Override
    public byte[] getFile(String fileId, String token, long version)
            throws UnexpectedErrorException, InvalidFileLocationException, InvalidTokenException {
        Log.info("getFile : " + fileId + "\n\t version: " + version + "\n token: " + token);
        validateToken(token, fileId);
        syncPoint.waitForVersion(version);
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
