package tp1.tokens;

import java.util.Arrays;
import java.util.Objects;

public class TemporaryToken implements Token {


    //token validity in seconds
    public static final long VALIDITY = 10;

    public static final String NAME = "Temp";

    public long getTimestamp() {
        return timestamp;
    }

    public String getFileId() {
        return fileId;
    }

    // timestamp in nano seconds
    private long timestamp;

    // file id this is valid for
    private String fileId;

    private String hash;


    private TemporaryToken(String fileId, long timestamp, String hash){
        this.timestamp = timestamp;
        this.hash = hash;
        this.fileId = fileId;
    }
    public TemporaryToken(String secret, String fileId, long timestamp){
        this.timestamp = timestamp;
        this.fileId = fileId;
        this.hash = generateHash(secret, fileId, timestamp);
    }

    public TemporaryToken(String secret, String fileId){
        this.timestamp = now();
        this.fileId = fileId;
        this.hash = generateHash(secret, fileId, timestamp);
    }

    public String getHash(){
        return hash;
    }

    private String generateHash(String secret, String fileId, long timestamp){
        return  Hash.of(secret, fileId, timestamp);
    }

    
    @Override
    public String tokenType() {
        return NAME;
    }

    public long expirationTimestamp(){
        return timestamp + VALIDITY * 1000;
    }

    /**
     * Current validity in seconds
     * @return the validity in seconds
     */
    public long currentValidity(){
        return expirationTimestamp() - now();
    }

    public boolean isExpired(){
        return currentValidity() <= 0;
    }

    /**
     * Current second
     * @return the current second
     */
    private static long now(){
        return System.currentTimeMillis();
    }

    @Override
    public boolean checkToken(String secret) {
        if(isExpired()){
            System.out.println("Expired");
            return false;
        }
        String correctHash = generateHash(secret, fileId, timestamp);
        return Objects.equals(this.hash, correctHash);
    }

    @Override
    public String[] toStrings() {
        return new String[]{Long.toString(timestamp), fileId, hash};
    }

    public static TemporaryToken fromStrings(String[] strings){
        return new TemporaryToken(strings[1], Long.parseLong(strings[0]), strings[2]);
    }
}
