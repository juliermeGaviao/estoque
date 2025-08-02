package br.com.dinamica.estoque.service.impl;

import br.com.dinamica.estoque.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nomeUsuario) throws UsernameNotFoundException {
        return this.usuarioRepository.findByNomeUsuario(nomeUsuario).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
