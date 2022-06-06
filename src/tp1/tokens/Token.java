package tp1.tokens;

public interface Token {

    String tokenType();

    boolean checkToken(String secret);

    String[] toStrings();
}
