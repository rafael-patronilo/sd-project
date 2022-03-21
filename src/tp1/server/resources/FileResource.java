package tp1.server.resources;

import tp1.api.service.rest.RestFiles;

public class FileResource implements RestFiles {
    @Override
    public void writeFile(String fileId, byte[] data, String token) {

    }

    @Override
    public void deleteFile(String fileId, String token) {

    }

    @Override
    public byte[] getFile(String fileId, String token) {
        return new byte[0];
    }
}
