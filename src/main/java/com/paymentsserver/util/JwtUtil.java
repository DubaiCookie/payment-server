package com.paymentsserver.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.paymentsserver.exception.InvalidTokenException;
import com.paymentsserver.exception.ExpiredTokenException;

@Component
public class JwtUtil {

    @Value("${jwt.secret:LOCAL_DEV_SECRET_KEY_CHANGE_ME}")
    private String secret;

    @Value("${jwt.issuer:user-server}")
    private String issuer;

    /**
     * 토큰 검증
     */
    public void validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new ExpiredTokenException("Token has expired");
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Invalid token");
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to extract user ID from token");
        }
    }

    /**
     * 토큰 타입 추출
     */
    public String getTokenType(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("type").asString();
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to extract token type");
        }
    }
}
