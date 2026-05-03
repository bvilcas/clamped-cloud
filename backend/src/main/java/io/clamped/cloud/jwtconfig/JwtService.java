package io.clamped.cloud.jwtconfig;

import io.clamped.cloud.user.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

// JWT Service. Also contains logic to build a JWT token so its basically JWT Token + Service
@Service
public class JwtService {
    private static final String SECRET_KEY =
            "ZmZmYTJiM2ZjNjYxNGMyZTYwNDg1ZmE1YjZjNTc3MGE3Njg4MTY2YzFhZWNmYWRmYjBlY2FkMmFiYjA2NzQ0ZA==";
    private static final long ACCESS_MINUTES = 15; // 15 minute JWT time

    public String generateAccessToken(UserPrincipal p) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("typ", "access");
        // sets roles of the userPrincipal into the jwt
        claims.put("roles", p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        return build(claims, p.getUsername(), Duration.ofMinutes(ACCESS_MINUTES), null);
    }


    // All the things within a JWT token
    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public String extractType(String token)     { return extractClaim(token, c -> (String)c.get("typ")); }
    public String extractJti(String jti)      { return extractClaim(jti, Claims::getId); } // we dont use
    public Date extractExpiration(String t)   { return extractClaim(t, Claims::getExpiration); }
    public Date extractIssuedAt(String t) { return extractClaim(t, Claims::getIssuedAt); }

    public boolean isTokenValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername()) && !isExpired(token);
    }

    public boolean isAccessToken(String token)  { return "access".equals(extractType(token)); }

    private boolean isExpired(String token) { return extractExpiration(token).before(new Date()); }

    private <T> T extractClaim(String token, Function<Claims,T> resolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(getSignInKey()).build()
                .parseClaimsJws(token).getBody();
        return resolver.apply(claims);
    }

    // custom build method for lombok
    private String build(Map<String,Object> claims, String subject, Duration ttl, String jti) {
        Instant now = Instant.now();
        return Jwts.builder() // specific Jwts builder that is not lombok
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .setId(jti) // null => omitted
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
