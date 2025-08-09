package br.com.dinamica.estoque.controller;

import java.util.Date;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.dto.AuthRequest;
import br.com.dinamica.estoque.dto.AuthResponse;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.impl.JwtService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UsuarioRepository usuarioRepository, PasswordEncoder encoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        var token = new UsernamePasswordAuthenticationToken(request.email(), request.senha());
        String jwt = this.jwtService.gerarToken(request.email());

    	this.authManager.authenticate(token);

        return new AuthResponse(jwt);
    }

    @PostMapping("/register")
    public void register(@RequestBody @Valid AuthRequest request) {
        var usuario = new Usuario(null, request.email(), this.encoder.encode(request.senha()), new Date(), new Date(), Set.of());

        this.usuarioRepository.save(usuario);
    }
}
