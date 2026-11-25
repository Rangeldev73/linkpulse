package dev.rangel.analyticsservice;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/** Issues HS256 tokens the same way the link-service does, for security tests. */
class JwtTestUtils {

    private static final String WRONG_SECRET_BASE64 = "aW52YWxpZC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtMjU2LWJpdHM=";

    private final JWSSigner signer;
    private final JWSSigner wrongSigner;

    JwtTestUtils(String base64Secret) throws Exception {
        this.signer = new MACSigner(Base64.getDecoder().decode(base64Secret));
        this.wrongSigner = new MACSigner(Base64.getDecoder().decode(WRONG_SECRET_BASE64));
    }

    String generateToken(String userId) throws Exception {
        return createToken(userId, Instant.now().plus(1, ChronoUnit.HOURS), signer);
    }

    String generateExpiredToken(String userId) throws Exception {
        return createToken(userId, Instant.now().minus(1, ChronoUnit.HOURS), signer);
    }

    String generateInvalidSignatureToken(String userId) throws Exception {
        return createToken(userId, Instant.now().plus(1, ChronoUnit.HOURS), wrongSigner);
    }

    private String createToken(String userId, Instant expiresAt, JWSSigner jwsSigner) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(jwsSigner);
        return jwt.serialize();
    }
}
