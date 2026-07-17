package com.exam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        Claims claims = parseToken(token);
        return claims != null && claims.getExpiration().after(new Date());
    }

    /**
     * 校验 token 有效性并返回解析后的 Claims，避免调用方重复解析。
     *
     * @return Claims 如果 token 有效; null 如果 token 无效或已过期
     */
    public Claims parseTokenIfValid(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.getExpiration().after(new Date())) {
            return claims;
        }
        return null;
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.get("userId").toString()) : null;
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username").toString() : null;
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role").toString() : null;
    }
}
