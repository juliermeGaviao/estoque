package br.com.dinamica.estoque.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.dto.AuthRequest;
import br.com.dinamica.estoque.dto.AuthResponse;
import br.com.dinamica.estoque.model.Usuario;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.impl.JwtService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
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
    public AuthResponse login(@RequestBody @Valid AuthRequest req) {
        var token = new UsernamePasswordAuthenticationToken(req.nomeUsuario(), req.senha());
        String jwt = this.jwtService.gerarToken(req.nomeUsuario());

        this.authManager.authenticate(token);

        return new AuthResponse(jwt);
    }

    @PostMapping("/register")
    public void register(@RequestBody @Valid AuthRequest req) {
        var usuario = new Usuario(null, "Novo Usuário", req.nomeUsuario(), this.encoder.encode(req.senha()));

        this.usuarioRepository.save(usuario);
    }
}
