package dev.rangel.linkservice.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTestUtils {

    private final JWSSigner signer;
    private final JWSSigner invalidSigner;

    public JwtTestUtils(@Value("${app.security.jwt.secret}") String base64Secret) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signer = new MACSigner(keyBytes);

        byte[] invalidKeyBytes = Base64.getDecoder().decode("aW52YWxpZC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtMjU2LWJpdHM=");
        this.invalidSigner = new MACSigner(invalidKeyBytes);
    }

    public String generateToken(String userId) throws Exception {
        return createToken(userId, Instant.now().plus(1, ChronoUnit.HOURS), signer);
    }

    public String generateExpiredToken(String userId) throws Exception {
        return createToken(userId, Instant.now().minus(1, ChronoUnit.HOURS), signer);
    }

    public String generateInvalidSignatureToken(String userId) throws Exception {
        return createToken(userId, Instant.now().plus(1, ChronoUnit.HOURS), invalidSigner);
    }

    private String createToken(String userId, Instant expirationTime, JWSSigner jwsSigner) throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expirationTime))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(jwsSigner);

        return signedJWT.serialize();
    }
}