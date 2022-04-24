package tp1.client.soap;

import tp1.api.service.soap.FilesException;
import tp1.api.service.soap.SoapFiles;
import tp1.client.ClientUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.server.soap.SoapUtils;

/**
 * Soap implementation for FilesServerClient
 */
public class SoapFilesClient implements FilesServerClient {
    private SoapFiles server;
    private String uri;
    public SoapFilesClient(String uri){
        this.uri = uri;
        server = ClientUtils.buildSoapClient(uri,
                SoapFiles.NAMESPACE, SoapFiles.NAME, SoapFiles.class);
    }

    @Override
    public String getFileDirectUrl(String fileId) {
        return uri + "/" + fileId;
    }

    @Override
    public synchronized void writeFile(String fileId, byte[] data, String token, int maxRetries) throws RequestTimeoutException {
        try {
            ClientUtils.reTry(()->{
                server.writeFile(fileId, data, token);
                return null;
            }, maxRetries);
        } catch (RequestTimeoutException e){
            throw e;
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void deleteFileAsync(String fileId, String token) {
        ClientUtils.reTryAsync(()->{
            try {
                server.deleteFile(fileId, token);
            } catch (FilesException e){
                if (e.getMessage().equals(SoapUtils.BAD_REQUEST)){
                    return false;
                }
            }
            return true;
        }, (b)->b);
    }

    @Override
    public void redirectToGetFile(String fileId, String token) {
        //Not implemented on soap
    }

    @Override
    public synchronized byte[] getFile(String fileId, String token) throws RequestTimeoutException, InvalidFileLocationException {
        try {
            return ClientUtils.reTry(()->{
                return server.getFile(fileId, token);
            });
        } catch (RequestTimeoutException e){
            throw e;
        } catch (Exception e) {
            if(e.getMessage() == null)
                throw new RuntimeException(e);
            if(e.getMessage().equals(SoapUtils.NOT_FOUND)){
                throw new InvalidFileLocationException();
            } else throw new RuntimeException(e);
        }
    }
}
