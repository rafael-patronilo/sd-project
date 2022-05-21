package tp1.common.services;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.UnexpectedErrorException;

import java.security.MessageDigest;

public class DropboxFilesService implements FilesService{
    private static final String API_KEY =                                                                                                                    "1tkx6s8ucnoi5sv";
    private static final String API_SECRET =                                                                                                                 "4bg7u7a0sgg2ssi";
    private static final String ACCESS_TOKEN_STR =                                                                                                            "sl.BHT1ZdjhlJSoox7OFCEUG16uLI5O5EvjwcZ2bCcJxyINr8-m_kEtQFrOynXlrNe99xTWoODUI1l4q9mvRbaW14Wnynvxy5bK6yPkxwm9_XIhI6V7PF8eeC-T75guBPD8Pwomxbg";

    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String DROPBOX_API_ARG_HDR = "Dropbox-API-Arg";

    private record UploadV1Args(String path, String mode, String content_hash){
        static final String OVERRIDE = "override";
        static final String URL = "https://content.dropboxapi.com/2/files/upload";
    }

    private record DeleteV2Args(String path){
        static final String URL = "https://api.dropboxapi.com/2/files/delete_v2";
    }



    private Gson json = new Gson();

    private MessageDigest digest;
    private OAuth2AccessToken accessToken = new OAuth2AccessToken(ACCESS_TOKEN_STR);
    private OAuth20Service service;

    public DropboxFilesService(){
        this.service = new ServiceBuilder(API_KEY).apiSecret(API_SECRET).build(DropboxApi20.INSTANCE);
        try{
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException {
        var upload = new OAuthRequest(Verb.POST, UploadV1Args.URL);
        upload.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

        var json_args = json.toJson(new UploadV1Args(fileId, UploadV1Args.OVERRIDE, "digest.digest(data)"/*TODO (or not)*/));
        upload.setPayload(json_args);

        service.signRequest(accessToken, upload);
    }

    @Override
    public void deleteFile(String fileId, String token) throws InvalidFileLocationException {

    }

    @Override
    public byte[] getFile(String fileId, String token) throws UnexpectedErrorException, InvalidFileLocationException {
        return new byte[0];
    }
}
