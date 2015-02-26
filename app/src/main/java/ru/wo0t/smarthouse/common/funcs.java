package ru.wo0t.smarthouse.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by alex on 2/26/15.
 */
public class funcs {
    public static final String md5(String toEncrypt) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("md5");
        digest.update(toEncrypt.getBytes());
        final byte[] bytes = digest.digest();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X",bytes[i]));
        }
        return sb.toString().toLowerCase();
    }
}
