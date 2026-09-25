package dev.rangel.linkservice.application.service;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class Base62CodeGenerator {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(BASE62_ALPHABET.length());
            sb.append(BASE62_ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}