package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.entity.ContatoFornecedor;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProviderContactMapper;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.service.ProviderContactService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProviderContactServiceImpl implements ProviderContactService {

	private ContatoFornecedorRepository repository;

	private FornecedorRepository fornecedorRepository;

	private ProviderContactMapper modelMapper;

	public ProviderContactServiceImpl(ContatoFornecedorRepository repository, FornecedorRepository fornecedorRepository, ProviderContactMapper modelMapper) {
		this.repository = repository;
		this.fornecedorRepository = fornecedorRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ProviderContactDto get(Long id) {
		ContatoFornecedor entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<ProviderContactDto> list(Long idFornecedor, Pageable pageable) {
        Specification<ContatoFornecedor> specification = (_, _, _) -> null;

        if (idFornecedor != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("fornecedor").get("id"), idFornecedor));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public ProviderContactDto save(ProviderContactDto dto, Usuario usuario) {
		ContatoFornecedor entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ContatoFornecedor();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		Fornecedor fornecedor = this.fornecedorRepository.findById(dto.getFornecedor().getId()).orElseThrow();
		
		entity.setFornecedor(fornecedor);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
