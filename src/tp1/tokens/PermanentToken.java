package tp1.tokens;

import java.util.Objects;

public class PermanentToken implements Token{
    public static final String NAME= "Permanent";

    private String secret;
    public PermanentToken(String secret){
        this.secret = secret;
    }

    @Override
    public String tokenType() {
        return NAME;
    }


    @Override
    public boolean checkToken(String secret) {
        return Objects.equals(this.secret, secret);
    }

    @Override
    public String[] toStrings() {
        return new String[]{secret};
    }

    public static PermanentToken fromStrings(String[] strings){
        return new PermanentToken(strings[0]);
    }
}
