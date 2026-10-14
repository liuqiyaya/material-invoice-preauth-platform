package com.liuqiya.invoice.utils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import io.jsonwebtoken.Claims;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours}")
    private long expireHours;

    //创建JWT
    public String genToken(Map<String, Object> claims) {
        long now = System.currentTimeMillis();//当前时间(毫秒)
        Date exp = new Date(now + expireHours * 60 * 60 * 1000);//过期时间 24小时(毫秒

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String jwt = Jwts.builder()
                .claims(claims)//存储数据
                .issuedAt(new Date(now))//签发时间
                .expiration(exp)//过期时间
                .signWith(key, Jwts.SIG.HS256)//签名算法
                .compact();
        return jwt;
    }

    //解析JWT
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }





}
