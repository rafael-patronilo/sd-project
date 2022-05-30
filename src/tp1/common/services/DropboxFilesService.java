package tp1.common.services;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import  com.github.scribejava.core.model.Response;
import com.google.gson.*;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.UnexpectedErrorException;

import java.io.IOException;
import java.util.logging.Logger;

public class DropboxFilesService implements FilesService{
    private static Logger Log = Logger.getLogger(DropboxFilesService.class.getName());
    private final String apiKey;
    private final String apiSecret;

    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final String DROPBOX_API_ARG_HDR = "Dropbox-API-Arg";
    private static final String DROPBOX_API_RESULT_HDR = "Dropbox-API-Result";

    private static final int SUCCESS_CODE = 200;
    private static final int ENDPOINT_ERROR_CODE = 409;
    private static final String ERROR_SUMMARY = "error_summary";
    private static final String NOT_FOUND = "path/not_found";
    private static final String DROPBOX_FOLDER = "sd2122_files";

    private record ApiResponse(int code, JsonElement response){
        boolean isNotFound(){
            return response.getAsJsonObject().get(ERROR_SUMMARY).getAsString().startsWith(NOT_FOUND);
        }
    }
    private record DownloadResponse(int code, JsonElement response, byte[] data){

        boolean isNotFound(){
            return response.getAsJsonObject().get(ERROR_SUMMARY).getAsString().startsWith(NOT_FOUND);
        }
    }

    private record UploadV1Args(String path, String mode){
        //TODO maybe add hash?
        static final String OVERWRITE = "overwrite";
        static final String URL = "https://content.dropboxapi.com/2/files/upload";
    }

    private record DeleteV2Args(String path){
        static final String URL = "https://api.dropboxapi.com/2/files/delete_v2";
    }

    private record DownloadV1Args(String path){
        static final String URL = "https://content.dropboxapi.com/2/files/download";
    }

    private record CreateFolderV2Args(String path){
        static final String URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    }

    private Gson json = new Gson();
    private OAuth2AccessToken accessToken;
    private OAuth20Service service;
    public DropboxFilesService(boolean cleanState, String apiKey, String apiSecret, String accessTokenStr){
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.accessToken = new OAuth2AccessToken(accessTokenStr);
        this.service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        initDropbox(cleanState);
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws UnexpectedErrorException {
        UploadV1Args args = new UploadV1Args(pathToFile(fileId), UploadV1Args.OVERWRITE);
        ApiResponse r = contentUpload(UploadV1Args.URL, args, data);
        if (r.code != SUCCESS_CODE){
            throw new UnexpectedErrorException();
        }
    }

    @Override
    public void deleteFile(String fileId, String token) throws InvalidFileLocationException {
        DeleteV2Args args = new DeleteV2Args(pathToFile(fileId));
        ApiResponse r = rpc(DeleteV2Args.URL, args);
        if(r.code == ENDPOINT_ERROR_CODE && r.isNotFound()){
            throw new InvalidFileLocationException();
        }
    }

    @Override
    public byte[] getFile(String fileId, String token) throws UnexpectedErrorException, InvalidFileLocationException {
        DownloadV1Args args = new DownloadV1Args(pathToFile(fileId));
        DownloadResponse r = contentDownload(DownloadV1Args.URL, args);

        if(r.code == ENDPOINT_ERROR_CODE && r.isNotFound()){
            throw new InvalidFileLocationException();
        } else if (r.code != SUCCESS_CODE){
            throw new UnexpectedErrorException();
        }
        return r.data();
    }

    private void initDropbox(boolean cleanState){
        Log.info("Initializing Dropbox");
        if(cleanState){
            Log.info("Cleaning old state");
            DeleteV2Args args = new DeleteV2Args("/" + DROPBOX_FOLDER);
            rpc(DeleteV2Args.URL, args);
        }
        Log.finest("Creating destination folder");
        CreateFolderV2Args args = new CreateFolderV2Args("/" + DROPBOX_FOLDER);
        rpc(CreateFolderV2Args.URL, args);
    }

    /**
     * Binding for dropbox api Content-download endpoints
     *
     * (Same as contentDownload but with empty body)
     *
     * @param url the api url
     * @param args the header args
     * @return the response
     */
    private DownloadResponse contentDownload(String url, Object args){
        OAuthRequest request = new OAuthRequest(Verb.POST, url);
        request.addHeader(CONTENT_TYPE_HDR, OCTET_STREAM_CONTENT_TYPE);

        String json_args = json.toJson(args);
        request.addHeader(DROPBOX_API_ARG_HDR, json_args);
        service.signRequest(accessToken, request);
        Log.info("content download endpoint request: url = " +
                url +
                ", args = " + json_args);

        try {
            Response r = service.execute(request);
            int code = r.getCode();
            String result;
            byte[] data;
            if(code == SUCCESS_CODE) {
                result = r.getHeader(DROPBOX_API_RESULT_HDR);
                data = r.getStream().readAllBytes();
            } else{
                result = r.getBody();
                data = null;
            }
            Log.info("Response (code " + code + "): " + result);
            JsonElement jsonResult;
            try{
                jsonResult = JsonParser.parseString(result);
            } catch (JsonSyntaxException | NullPointerException ignored){
                jsonResult = null;
            }

            return new DownloadResponse(code, jsonResult, data);
        } catch (Exception e){
            Log.severe(e.toString());
            return new DownloadResponse(-1, null, null);
        }
    }

    /**
     * Binding for dropbox api Content-upload endpoints
     *
     * @param url the api url
     * @param args the header args
     * @param data the payload data
     * @return the response
     */
    private ApiResponse contentUpload(String url, Object args, byte[] data) {
        OAuthRequest request = new OAuthRequest(Verb.POST, url);
        request.addHeader(CONTENT_TYPE_HDR, OCTET_STREAM_CONTENT_TYPE);

        String json_args = json.toJson(args);
        request.addHeader(DROPBOX_API_ARG_HDR, json_args);
        request.setPayload(data);
        Log.info("content upload endpoint request: url = " +
                 url +
                ", args = " + json_args +
                ", length = " + data.length);

        return executeRequest(request);
    }

    /**
     * Binding for dropbox api RPC endpoints
     *
     * @param url the api url
     * @param args the payload args
     * @return the response
     */
    private ApiResponse rpc(String url, Object args) {
        OAuthRequest request = new OAuthRequest(Verb.POST, url);
        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

        String json_args = json.toJson(args);
        request.setPayload(json_args);
        Log.info("rpc endpoint request: url = " +
                url +
                ", args = " + json_args);
        
        return executeRequest(request);
    }
    
    private ApiResponse executeRequest(OAuthRequest request){
        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            int code = r.getCode();
            String body = r.getBody();
            JsonElement jsonBody;
            try{
                jsonBody = JsonParser.parseString(body);
            } catch (JsonSyntaxException ignored){
                jsonBody = null;
            }
            Log.info("Response (code " + r.getCode() + "): " + r.getBody());
            return new ApiResponse(code, jsonBody);
        } catch (Exception e){
            Log.severe(e.toString());
            return new ApiResponse(-1, null);
        }
    }

    /**
     * Returns the path to the file on dropbox
     * @param fileId the file's id
     * @return the file's path on dropbox
     */
    private String pathToFile(String fileId){
        return "/" + DROPBOX_FOLDER + "/" + fileId;
    }
}
