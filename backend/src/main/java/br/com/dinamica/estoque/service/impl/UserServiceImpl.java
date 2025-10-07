package br.com.dinamica.estoque.service.impl;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.exception.ConstraintViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.configuration.auth.SHA256PasswordEncoder;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserListDto;
import br.com.dinamica.estoque.dto.UserRequestDTO;
import br.com.dinamica.estoque.entity.Perfil;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.PerfilRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import br.com.dinamica.estoque.service.UserService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class UserServiceImpl implements UserDetailsService, UserService {

    private UsuarioRepository usuarioRepository;

    private PerfilRepository perfilRepository;

    private UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository;

    private SHA256PasswordEncoder passwordEncoder;
    
	private ModelMapper modelMapper;

    public UserServiceImpl(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository,
    		SHA256PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioTabelaPrecoRepository = usuarioTabelaPrecoRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    	return this.usuarioRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

	@Override
	public UserDto getUser(Long id) {
		Usuario usuario = this.usuarioRepository.findById(id).orElseThrow();

		return this.modelMapper.map(usuario, UserDto.class);
	}

    @Override
    public Page<UserListDto> list(String email, Pageable pageable) {
        Specification<Usuario> specification = (root, query, cb) -> null;

        if (email != null && !email.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }

        specification = specification.and((root, query, cb) -> cb.notEqual(root.get("id"), 1L));
        specification = specification.and((root, query, cb) -> cb.equal(root.get("ativo"), Boolean.TRUE));

        return this.usuarioRepository.findAll(specification, pageable).map(usuario -> {
        	UserListDto result = this.modelMapper.map(usuario, UserListDto.class);

        	result.setPerfis(usuario.getPerfis().stream().map(Perfil::getNome).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ")));
        	result.setTabelas(this.usuarioTabelaPrecoRepository.findByVendedor(usuario.getId()).stream().map(linha -> linha.getTabela().getNome()).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ")));

        	return result;
        });
    }

    @Override
    public UserDto save(UserRequestDTO dto, Usuario usuarioLogado) {
        Usuario usuario = new Usuario();
        Date agora = DateUtil.now();

        if (dto.getId() != null) {
        	usuario = this.usuarioRepository.findById(dto.getId()).orElseThrow();
        } else {
        	Optional<Usuario> outroUsuario = this.usuarioRepository.findByEmail(dto.getEmail());

        	if (outroUsuario.isPresent()) {
        		throw new ConstraintViolationException(null, null, null);
        	}

        	usuario.setDataCriacao(agora);
        	usuario.setAtivo(true);
        }

        usuario.setEmail(dto.getEmail());

        if (dto.getSenha() != null) {
        	usuario.setSenha(this.passwordEncoder.encode(dto.getSenha()));
        }

        usuario.setDataAlteracao(agora);

        Set<Perfil> perfis = dto.getPerfis().stream().map(this.perfilRepository::findById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());

        usuario.setCadastrante(usuarioLogado);
        usuario.setPerfis(perfis);

        usuario = this.usuarioRepository.saveAndFlush(usuario);

        return this.modelMapper.map(usuario, UserDto.class);
    }

    @Override
    public UserDto changePassword(UserRequestDTO dto) {
    	Usuario usuario = this.usuarioRepository.findById(dto.getId()).orElseThrow();

    	usuario.setSenha(this.passwordEncoder.encode(dto.getSenha()));
    	usuario.setDataAlteracao(DateUtil.now());

        return this.modelMapper.map(usuario, UserDto.class);
    }

	@Override
	public void delete(Long id) {
		Usuario usuario = this.usuarioRepository.findById(id).orElseThrow();

		usuario.setAtivo(false);

		this.usuarioRepository.save(usuario);
	}

}
