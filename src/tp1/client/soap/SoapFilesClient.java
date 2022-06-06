package tp1.client.soap;

import tp1.api.service.soap.FilesException;
import tp1.api.service.soap.SoapFiles;
import tp1.client.ClientUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.server.soap.SoapUtils;
import tp1.tokens.TokenManager;

/**
 * Soap implementation for FilesServerClient
 */
public class SoapFilesClient implements FilesServerClient {
    private final String permanentToken = TokenManager.serializeToken(TokenManager.createPermanentToken());
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
    public String getURI() {
        return uri;
    }

    @Override
    public synchronized void writeFile(String fileId, byte[] data, int maxRetries) throws RequestTimeoutException {
        try {
            ClientUtils.reTry(()->{
                server.writeFile(fileId, data, permanentToken);
                return null;
            }, maxRetries);
        } catch (RequestTimeoutException e){
            throw e;
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void deleteFileAsync(String fileId) {
        ClientUtils.reTryAsync(()->{
            try {
                server.deleteFile(fileId, permanentToken);
            } catch (FilesException e){
                if (e.getMessage().equals(SoapUtils.BAD_REQUEST)){
                    return false;
                }
            }
            return true;
        }, (b)->b);
    }

    @Override
    public void redirectToGetFile(String fileId) {
        redirectToGetFile(fileId,-1L);
    }

    @Override
    public void redirectToGetFile(String fileId, long version) {
        //Not implemented on soap
    }

    @Override
    public byte[] getFile(String fileId) throws RequestTimeoutException, InvalidFileLocationException {
        return getFile(fileId, -1L);
    }

    @Override
    public synchronized byte[] getFile(String fileId, long version) throws RequestTimeoutException, InvalidFileLocationException {
        try {
            return ClientUtils.reTry(()->{
                return server.getFile(fileId, permanentToken);
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
