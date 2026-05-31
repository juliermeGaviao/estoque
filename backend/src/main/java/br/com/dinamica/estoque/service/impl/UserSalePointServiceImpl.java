package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioPontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.UserSalePointService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class UserSalePointServiceImpl implements UserSalePointService {

	private UsuarioPontoVendaRepository repository;

	private PontoVendaRepository pontoVendaRepository;

	private UsuarioRepository usuarioRepository;

	private ModelMapper modelMapper;

	public UserSalePointServiceImpl(UsuarioPontoVendaRepository repository, PontoVendaRepository pontoVendaRepository, UsuarioRepository usuarioRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.usuarioRepository = usuarioRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public UserSalePointDto get(Long id) {
		UsuarioPontoVenda entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, UserSalePointDto.class);
	}

	@Override
	public Page<UserSalePointDto> list(Long idUsuario, Long idPontoVenda, Pageable pageable) {
        Specification<UsuarioPontoVenda> specification = (root, query, cb) -> null;

        if (idUsuario != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), idUsuario));
        }

        if (idPontoVenda != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("pontoVenda").get("id"), idPontoVenda));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, UserSalePointDto.class));
	}

	@Override
	public UserSalePointDto save(UserSalePointDto dto, Usuario usuario) {
		UsuarioPontoVenda entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new UsuarioPontoVenda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(this.usuarioRepository.findById(dto.getUsuario().getId()).orElseThrow());
		entity.setPontoVenda(this.pontoVendaRepository.findById(dto.getPontoVenda().getId()).orElseThrow());
		entity.setUsuarioCadastro(usuario);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, UserSalePointDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
