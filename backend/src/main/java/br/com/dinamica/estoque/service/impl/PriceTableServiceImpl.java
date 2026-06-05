package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PriceTableMapper;
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

	private PriceTableMapper modelMapper;

	public PriceTableServiceImpl(TabelaPrecoRepository repository, TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository,
			UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository, PriceTableMapper modelMapper) {
		this.repository = repository;
		this.tabelaPrecoProdutoRepository = tabelaPrecoProdutoRepository;
		this.usuarioTabelaPrecoRepository = usuarioTabelaPrecoRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PriceTableDto get(Long id) {
		TabelaPreco entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<PriceTableDto> list(String nome, Pageable pageable) {
        Specification<TabelaPreco> specification = (_, _, _) -> null;

        if (nome != null && !nome.isBlank()) {
            specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public PriceTableDto save(PriceTableDto dto, Usuario usuario) {
		TabelaPreco entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TabelaPreco();

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
		this.tabelaPrecoProdutoRepository.deleteByTabela_Id(id);
		this.usuarioTabelaPrecoRepository.deleteByTabela_Id(id);
		
		this.repository.deleteById(id);
	}

	@Override
	public void save(List<PriceTableDto> dtos, Usuario usuario) {
		dtos.forEach(dto -> this.save(dto, usuario));
	}

}
