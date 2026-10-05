package inpg.sn.lms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import inpg.sn.lms.auth.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration}")
    private long expirationTime;
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Générer un Token pour l'utilisateur
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toList()));
        claims.put("sessionVersion", user.getSessionVersion());

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    // 2. Extraire le nom d'utilisateur (username) du Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. Extraire la date d'expiration
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 5. Valider le Token avec les détails de l'utilisateur
    public Boolean validateToken(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            Object version = claims.get("sessionVersion");
            return user != null && user.getUsername().equals(claims.getSubject())
                    && (version instanceof Integer || version instanceof Long)
                    && ((Number) version).longValue() == user.getSessionVersion()
                    && claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
