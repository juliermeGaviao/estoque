package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.SaleService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class SaleServiceImpl implements SaleService {

	private VendaRepository repository;

	private ItemVendaRepository itemVendaRepository;

	private UsuarioRepository usuarioRepository;

	private ModelMapper modelMapper;

	public SaleServiceImpl(VendaRepository repository, ItemVendaRepository itemVendaRepository, UsuarioRepository usuarioRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.itemVendaRepository = itemVendaRepository;
		this.usuarioRepository = usuarioRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<SaleDto, Venda>() {
            @Override
            protected void configure() {
                skip(destination.getVendedor());
            }
        });
	}

	@Override
	public SaleDto get(Long id) {
		Venda entity = this.repository.findById(id).orElseThrow();

		entity.getVendedor().setPerfis(null);

		return this.modelMapper.map(entity, SaleDto.class);
	}

	private static final String DISCOUNT_FIELD = "desconto";

	@Override
	public Page<SaleDto> list(Long vendedorId, Float minDesconto, Float maxDesconto, String observacoes, Pageable pageable) {
        Specification<Venda> specification = (root, query, cb) -> null;

        if (vendedorId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("vendedor").get("id"), vendedorId));
        }

        if (minDesconto != null && maxDesconto != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(DISCOUNT_FIELD), minDesconto, maxDesconto));
        } else if (minDesconto != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DISCOUNT_FIELD), minDesconto));
        } else if (maxDesconto != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(DISCOUNT_FIELD), maxDesconto));
        }

        if (observacoes != null && !observacoes.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + observacoes.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(entity -> {
			SaleDto result = this.modelMapper.map(entity, SaleDto.class);

			result.getVendedor().setPerfis(null);

			return result;
		});
	}

	@Override
	public SaleDto save(SaleDto dto, Usuario usuario) {
		Venda entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Venda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		Usuario vendedor = this.usuarioRepository.findById(dto.getVendedor().getId()).orElseThrow();

		entity.setVendedor(vendedor);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, SaleDto.class);
	}

	@Override
	public void delete(Long id) {
		this.itemVendaRepository.deleteByVenda_Id(id);
		this.repository.deleteById(id);
	}

}
