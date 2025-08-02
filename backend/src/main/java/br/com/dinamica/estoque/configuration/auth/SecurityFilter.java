package br.com.dinamica.estoque.configuration.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.dinamica.estoque.service.impl.JwtService;
import br.com.dinamica.estoque.service.impl.UserDetailsServiceImpl;

import java.io.IOException;

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var token = getToken((HttpServletRequest) request);

        if (token != null) {
            var subject = this.jwtService.getSubject(token);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var usuario = this.userDetailsService.loadUserByUsername(subject);
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

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
