package Utils;

import org.mindrot.jbcrypt.BCrypt;

public class TestHash {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("expert123", BCrypt.gensalt(12));
        System.out.println("Hash bcrypt : " + hash);
    }
}

