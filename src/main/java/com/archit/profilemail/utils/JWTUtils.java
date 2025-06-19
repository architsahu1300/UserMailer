package com.archit.profilemail.utils;

import com.archit.profilemail.model.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.pbkdf2.SHA256Digest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JWTUtils {
    private final String SECRET_KEY = "MySecretKeyJWT@13090501";
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateToken(UserDetails userDetails){
        long jwtExpirationMs = 60 * 60 * 1000;
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .setIssuedAt(new Date())
                .compact();
    }
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    public boolean isTokenValid(String token, UserDetails userDetails){
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }
}
