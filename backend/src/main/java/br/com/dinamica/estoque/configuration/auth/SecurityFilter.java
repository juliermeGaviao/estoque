package br.com.dinamica.estoque.configuration.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.service.impl.JwtService;
import br.com.dinamica.estoque.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityFilter extends GenericFilter {

    private static final long serialVersionUID = -3894446815946158380L;

    private final transient JwtService jwtService;
    private final transient UserDetailsServiceImpl userDetailsService;

    public SecurityFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        var token = getToken(httpRequest);
        if (token != null) {
            var subject = this.jwtService.getSubject(token);
            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var usuario = this.userDetailsService.loadUserByUsername(subject);
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
