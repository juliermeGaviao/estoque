package br.com.dinamica.estoque.configuration.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.dinamica.estoque.service.impl.JwtService;
import br.com.dinamica.estoque.service.impl.UsuarioDetailsServiceImpl;

import java.io.IOException;

@Component
public class SecurityFilter extends GenericFilter {

    private static final long serialVersionUID = -3894446815946158380L;

    private final transient JwtService jwtService;
    
    private final transient UsuarioDetailsServiceImpl usuarioDetailsService;

    public SecurityFilter(JwtService jwtService, UsuarioDetailsServiceImpl usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        var request = (HttpServletRequest) req;
        var token = getToken(request);

        if (token != null) {
            var subject = this.jwtService.getSubject(token);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var usuario = this.usuarioDetailsService.loadUserByUsername(subject);
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(req, res);
    }

    private String getToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
