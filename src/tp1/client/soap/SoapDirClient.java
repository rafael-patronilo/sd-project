package tp1.client.soap;

import tp1.api.service.soap.DirectoryException;
import tp1.api.service.soap.SoapDirectory;
import tp1.client.ClientUtils;
import tp1.common.clients.DirServerClient;
import tp1.server.soap.SoapUtils;
import tp1.tokens.TokenManager;

/**
 * Soap implementation for DirServerClient
 */
public class SoapDirClient implements DirServerClient {
    private final String permanentToken = TokenManager.serializeToken(TokenManager.createPermanentToken());

    private SoapDirectory server;
    public SoapDirClient(String uri){
        server = ClientUtils.buildSoapClient(uri,
                SoapDirectory.NAMESPACE, SoapDirectory.NAME, SoapDirectory.class);
    }
    @Override
    public synchronized void deleteDirectoryAsync(String userId) {
        ClientUtils.reTryAsync(()-> {
            try {
                server.deleteDirectory(userId, "", permanentToken);
            } catch (DirectoryException e) {
                if (e.getMessage().equals(SoapUtils.BAD_REQUEST)){
                     return false;
                }
            }
            return true;
        }, (b) -> b);
    }
}
