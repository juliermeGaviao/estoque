package br.com.dinamica.estoque.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;
import br.com.dinamica.estoque.mapper.UserSalePointMapper;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioPontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.UserSalePointService;

@Service
public class UserSalePointServiceImpl implements UserSalePointService {

	private UsuarioPontoVendaRepository repository;

	private PontoVendaRepository pontoVendaRepository;

	private UsuarioRepository usuarioRepository;

	private UserSalePointMapper modelMapper;

	public UserSalePointServiceImpl(UsuarioPontoVendaRepository repository, PontoVendaRepository pontoVendaRepository, UsuarioRepository usuarioRepository, UserSalePointMapper modelMapper) {
		this.repository = repository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.usuarioRepository = usuarioRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public UserSalePointDto get(Long id) {
		UsuarioPontoVenda entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<UserSalePointDto> list(Long idUsuario, Long idPontoVenda, Pageable pageable) {
        Specification<UsuarioPontoVenda> specification = (_, _, _) -> null;

        if (idUsuario != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("usuario").get("id"), idUsuario));
        }

        if (idPontoVenda != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("pontoVenda").get("id"), idPontoVenda));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public UserSalePointDto save(UserSalePointDto dto) {
		this.repository.deleteByUsuario(dto.getUsuario().getId());

		return this.modelMapper.toDto(this.saveSalePoint(dto));
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public void saveSalePoints(List<UserSalePointDto> dtos) {
		this.repository.deleteByUsuario(dtos.get(0).getUsuario().getId());

		dtos.forEach(this::saveSalePoint);
	}

	private UsuarioPontoVenda saveSalePoint(UserSalePointDto dto) {
		UsuarioPontoVenda entity = new UsuarioPontoVenda();

		entity.setUsuario(this.usuarioRepository.findById(dto.getUsuario().getId()).orElseThrow());
		entity.setPontoVenda(this.pontoVendaRepository.findById(dto.getPontoVenda().getId()).orElseThrow());

		return this.repository.saveAndFlush(entity);

	}

}
