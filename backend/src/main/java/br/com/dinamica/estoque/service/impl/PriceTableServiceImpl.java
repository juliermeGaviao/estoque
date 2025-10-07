package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import br.com.dinamica.estoque.service.PriceTableService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PriceTableServiceImpl implements PriceTableService {

	private TabelaPrecoRepository repository;

	private TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository;

	private UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository;

	private ModelMapper modelMapper;

	public PriceTableServiceImpl(TabelaPrecoRepository repository, TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository,
			UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.tabelaPrecoProdutoRepository = tabelaPrecoProdutoRepository;
		this.usuarioTabelaPrecoRepository = usuarioTabelaPrecoRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PriceTableDto get(Long id) {
		TabelaPreco entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, PriceTableDto.class);
	}

	@Override
	public Page<PriceTableDto> list(String nome, Pageable pageable) {
        Specification<TabelaPreco> specification = (root, query, cb) -> null;

        if (nome != null && !nome.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, PriceTableDto.class));
	}

	@Override
	public PriceTableDto save(PriceTableDto dto, Usuario usuario) {
		TabelaPreco entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TabelaPreco();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, PriceTableDto.class);
	}

	@Override
	public void delete(Long id) {
		this.tabelaPrecoProdutoRepository.deleteByTabela_Id(id);
		this.usuarioTabelaPrecoRepository.deleteByTabela_Id(id);
		
		this.repository.deleteById(id);
	}

}
