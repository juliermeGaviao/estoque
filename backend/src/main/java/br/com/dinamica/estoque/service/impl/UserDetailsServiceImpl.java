package br.com.dinamica.estoque.service.impl;

import java.util.NoSuchElementException;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserListDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.UserService;

@Service
public class UserDetailsServiceImpl implements UserDetailsService, UserService {

    private UsuarioRepository usuarioRepository;

	private ModelMapper modelMapper;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository, ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    	return this.usuarioRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

	@Override
	public UserDto getUser(Long id) throws NoSuchElementException {
		Usuario usuario = this.usuarioRepository.findById(id).orElseThrow();

		return this.modelMapper.map(usuario, UserDto.class);
	}

    @Override
    public Page<UserListDto> list(String email, Pageable pageable) {
        Specification<Usuario> specification = (root, query, cb) -> null;

        if (email != null && !email.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }

        return this.usuarioRepository.findAll(specification, pageable).map(usuario -> this.modelMapper.map(usuario, UserListDto.class));
    }
}
