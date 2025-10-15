package br.com.dinamica.estoque.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import br.com.dinamica.estoque.service.UserPriceTableService;

@Service
public class UserPriceTableServiceImpl implements UserPriceTableService {

	private UsuarioTabelaPrecoRepository repository;

	private TabelaPrecoRepository tabelaPrecoRepository;

	private UsuarioRepository usuarioRepository;

	private ModelMapper modelMapper;

	public UserPriceTableServiceImpl(UsuarioTabelaPrecoRepository repository, TabelaPrecoRepository tabelaPrecoRepository, UsuarioRepository usuarioRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.tabelaPrecoRepository = tabelaPrecoRepository;
		this.usuarioRepository = usuarioRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<UsuarioTabelaPreco, UserPriceTableDto>() {
            @Override
            protected void configure() {
                skip(destination.getUsuario().getPerfis());
            }
        });
	}

	@Override
	public UserPriceTableDto get(Long id) {
		UsuarioTabelaPreco entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, UserPriceTableDto.class);
	}

	@Override
	public Page<UserPriceTableDto> list(Long idTabelaPreco, Long idVendedor, Pageable pageable) {
        Specification<UsuarioTabelaPreco> specification = (root, query, cb) -> null;

        if (idTabelaPreco != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("tabela").get("id"), idTabelaPreco));
        }

        if (idVendedor != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), idVendedor));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, UserPriceTableDto.class));
	}

	@Override
	public UserPriceTableDto save(UserPriceTableDto dto) {
		this.repository.deleteByUsuario(dto.getUsuario().getId());

		return this.modelMapper.map(this.saveTable(dto), UserPriceTableDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public void saveTables(List<UserPriceTableDto> dtos) {
		this.repository.deleteByUsuario(dtos.get(0).getUsuario().getId());

		dtos.forEach(this::saveTable);
	}

	private UsuarioTabelaPreco saveTable(UserPriceTableDto dto) {
		UsuarioTabelaPreco entity = new UsuarioTabelaPreco();

		TabelaPreco tabela = this.tabelaPrecoRepository.findById(dto.getTabela().getId()).orElseThrow();
		Usuario usuario = this.usuarioRepository.findById(dto.getUsuario().getId()).orElseThrow();

		entity.setTabela(tabela);
		entity.setUsuario(usuario);

		return this.repository.saveAndFlush(entity);

	}

}
