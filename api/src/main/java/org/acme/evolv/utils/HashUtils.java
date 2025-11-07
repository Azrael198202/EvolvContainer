package org.acme.evolv.utils;

import java.security.MessageDigest;
import org.mindrot.jbcrypt.BCrypt;

public final class HashUtils {
    private HashUtils() {
    }

    public static String hashString(String input, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = md.digest(input.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String crypString(String input , int salt){
        return BCrypt.hashpw(input, BCrypt.gensalt(salt));
    }

    public static boolean checkCryp(String input , String hashed){
        return BCrypt.checkpw(input, hashed);
    }
}
