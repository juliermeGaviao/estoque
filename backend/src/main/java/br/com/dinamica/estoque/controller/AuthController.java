package br.com.dinamica.estoque.controller;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UsuarioRepository usuarioRepository, PasswordEncoder encoder, UserDetailsService userDetailsService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        var token = new UsernamePasswordAuthenticationToken(request.email(), request.senha());

    	this.authManager.authenticate(token);

        String jwt = this.jwtService.gerarToken(request.email());

        Usuario usuario = (Usuario) this.userDetailsService.loadUserByUsername(request.email());
        List<String> perfis = usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return new AuthResponse(usuario.getId(), jwt, perfis.toArray(new String[0]));
    }

    @PostMapping("/register")
    public void register(@RequestBody @Valid AuthRequest request, @AuthenticationPrincipal Usuario usuarioLogado) {
        var usuario = new Usuario(null, request.email(), this.encoder.encode(request.senha()), true, usuarioLogado, new Date(), new Date(), Set.of());

        this.usuarioRepository.save(usuario);
    }

}
