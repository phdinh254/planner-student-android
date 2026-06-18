package com.example.personalplanner.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordUtils {

    public static String hashPassword(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder stringBuilder = new StringBuilder();

            for (byte b : hashBytes) {
                stringBuilder.append(String.format("%02x", b));
            }

            return stringBuilder.toString();

        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
