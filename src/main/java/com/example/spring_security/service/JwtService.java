package com.example.spring_security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

///  This core component is responsible for
///  - Generating tokens
///  - parsing tokens - breaking down token to readable parts
///  - Validating tokens
///  - Extracting claims
///  - Uses `@Value` to inject properties from `application.yml`

@Service
@Getter
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;


    @Value("${jwt.access-token-expiration}")
    @Getter
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // Generate Secret key
    private SecretKey getSigningKey(){
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //Token Generation
    public String generateToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims,
            long expiration
    ){
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Access Token Generation
    public String generateAccessToken(UserDetails userDetails){
        return generateToken(userDetails, new HashMap<>(), accessTokenExpiration);
    }

    // Refresh token Generation
    public String generateRefreshToken(UserDetails userDetails){
        return generateToken(userDetails, new HashMap<>(), refreshTokenExpiration);
    }

    // claim Extraction Methods
    public <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    // Extract Username from Token
    public String extractUserName(String token){
        return extractClaim(token, Claims::getSubject);
    }

    // Token Validation
    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUserName(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token) ;
    }

    // Check Token expiration
    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    // Extract Expiration Date
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract All claims
    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
