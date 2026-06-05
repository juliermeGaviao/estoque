package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProviderMapper;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.service.ProviderService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProviderServiceImpl implements ProviderService {

	private FornecedorRepository repository;

	private ContatoFornecedorRepository contatoFornecedorRepository;

	private ProviderMapper modelMapper;

	public ProviderServiceImpl(FornecedorRepository repository, ContatoFornecedorRepository contatoFornecedorRepository, ProviderMapper modelMapper) {
		this.repository = repository;
		this.contatoFornecedorRepository = contatoFornecedorRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ProviderDto get(Long id) {
		Fornecedor entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<ProviderDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable) {
        Specification<Fornecedor> specification = (_, _, _) -> null;

        if (razaoSocial != null && !razaoSocial.isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("razaoSocial")), "%" + razaoSocial.toLowerCase() + "%"));
        }

        if (fantasia != null && !fantasia.isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("fantasia")), "%" + fantasia.toLowerCase() + "%"));
        }

        if (cnpj != null && !cnpj.isBlank()) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("cnpj"), cnpj));
        }

        if (fone != null && !fone.isBlank()) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("fone"), fone));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public ProviderDto save(ProviderDto dto, Usuario usuario) {
		Fornecedor entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Fornecedor();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.contatoFornecedorRepository.deleteByFornecedor_Id(id);
		this.repository.deleteById(id);
	}

}
