package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.mapper.SaleMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.SaleService;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;
import jakarta.transaction.Transactional;

@Service
public class SaleServiceImpl implements SaleService {

	private VendaRepository repository;

	private ItemVendaRepository itemVendaRepository;

	private UsuarioRepository usuarioRepository;

	private TabelaPrecoRepository precoTabelaRepository;

	private ClienteRepository clienteRepository;

	private PontoVendaRepository pontoVendaRepository;

	private StockService stockService;

	private SaleMapper modelMapper;

	public SaleServiceImpl(VendaRepository repository, ItemVendaRepository itemVendaRepository, UsuarioRepository usuarioRepository, TabelaPrecoRepository precoTabelaRepository, ClienteRepository clienteRepository, PontoVendaRepository pontoVendaRepository, StockService stockService, SaleMapper modelMapper) {
		this.repository = repository;
		this.itemVendaRepository = itemVendaRepository;
		this.usuarioRepository = usuarioRepository;
		this.precoTabelaRepository = precoTabelaRepository;
		this.clienteRepository = clienteRepository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.stockService = stockService;
		this.modelMapper = modelMapper;
	}

	@Override
	public SaleDto get(Long id) {
		Venda entity = this.repository.findById(id).orElseThrow();
		SaleDto result = this.modelMapper.toDto(entity);

		result.getVendedor().setPerfis(null);

		return result;
	}

	private static final String DISCOUNT_FIELD = "desconto";

	@Override
	public Page<SaleDto> list(Long idCliente, Long idVendedor, Float minDesconto, Float maxDesconto, String observacoes, Pageable pageable) {
        Specification<Venda> specification = (_, _, _) -> null;

        if (idCliente != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("cliente").get("id"), idCliente));
        }

        if (idVendedor != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("vendedor").get("id"), idVendedor));
        }

        if (minDesconto != null && maxDesconto != null) {
            specification = specification.and((root, _, cb) -> cb.between(root.get(DISCOUNT_FIELD), minDesconto, maxDesconto));
        } else if (minDesconto != null) {
            specification = specification.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get(DISCOUNT_FIELD), minDesconto));
        } else if (maxDesconto != null) {
            specification = specification.and((root, _, cb) -> cb.lessThanOrEqualTo(root.get(DISCOUNT_FIELD), maxDesconto));
        }

        if (observacoes != null && !observacoes.isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("observacoes")), "%" + observacoes.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(entity -> {
			SaleDto result = this.modelMapper.toDto(entity);

			result.getVendedor().setPerfis(null);

			return result;
		});
	}

	@Override
	public SaleDto save(SaleDto dto, Usuario usuario) {
		Venda entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Venda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		Cliente cliente = null;
		if (dto.getCliente() != null && dto.getCliente().getId() != null) {
			cliente = this.clienteRepository.findById(dto.getCliente().getId()).orElseThrow();
		}
		Usuario vendedor = this.usuarioRepository.findById(dto.getVendedor().getId()).orElseThrow();
		TabelaPreco tabela = this.precoTabelaRepository.findById(dto.getTabela().getId()).orElseThrow();
		PontoVenda ponto = this.pontoVendaRepository.findById(dto.getPontoVenda().getId()).orElseThrow();

		entity.setCliente(cliente);
		entity.setVendedor(vendedor);
		entity.setTabela(tabela);
		entity.setPontoVenda(ponto);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	@Transactional
	public void delete(Long id, Usuario usuario) {
		this.stockService.undoSale(id, usuario);
		this.itemVendaRepository.deleteByVenda_Id(id);
		this.repository.deleteById(id);
	}

}
