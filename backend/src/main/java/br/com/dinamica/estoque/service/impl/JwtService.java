package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final long EXPIRATION = 86400000; // 24 horas

	@Value("${jwt.secret}")
	private String jwtSecret;

	public String gerarToken(String subject) {
		return Jwts.builder().setSubject(subject).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
				.signWith(Keys.hmacShaKeyFor(this.jwtSecret.getBytes())).compact();
	}

	public String getSubject(String token) {
		return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(this.jwtSecret.getBytes())).build().parseClaimsJws(token).getBody().getSubject();
	}

}
