package br.com.dinamica.estoque.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.dto.PageResponse;
import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.PriceTableProductFilterDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.PriceTableProductService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/price-table-product")
@Slf4j
public class PriceTableProductController {

	private static final String ENTITY = "Produto na Tabela de preços";
	private static final String ENTITIES = "Produtos nas Tabelas de preços";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private PriceTableProductService service;

	public PriceTableProductController(PriceTableProductService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<Object> get(@RequestParam Long id) {
		try {
			return ResponseEntity.ok(this.service.get(id));
		} catch (NoSuchElementException e) {
			String mensagem = NOT_FOUND + id;
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		}
	}

	@GetMapping("/list")
	public ResponseEntity<Object> list(
			@RequestParam(required = false) Long idTabelaPreco,
			@RequestParam(required = false) Long idProduto,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<PriceTableProductDto> result = this.service.list(idTabelaPreco, idProduto, pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody PriceTableProductDto dto, @AuthenticationPrincipal Usuario usuario) {
		try {
			return ResponseEntity.ok(this.service.save(dto, usuario));
		} catch (NoSuchElementException e) {
			String mensagem = NOT_FOUND + dto.getId();
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		} catch (RuntimeException e) {
			String mensagem = "Erro ao salvar " + ENTITY.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@DeleteMapping
	public ResponseEntity<Object> delete(@RequestParam Long id) {
		try {
			this.service.delete(id);

			return ResponseEntity.ok().build();
		} catch (NoSuchElementException e) {
			String mensagem = NOT_FOUND + id;
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		} catch (RuntimeException e) {
			String mensagem = "Erro ao remover " + ENTITY.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@GetMapping("/list-product")
	public ResponseEntity<Object> listProducts(
			@RequestParam(required = false) Long idTabelaPreco,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String referencia,
			@RequestParam(required = false) Long idTipoProduto,
			@RequestParam(required = false) Long idFornecedor,
			@RequestParam(required = false) Integer minPeso,
			@RequestParam(required = false) Integer maxPeso,
			@RequestParam(required = false) Integer minPreco,
			@RequestParam(required = false) Integer maxPreco,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<PriceTableProductDto> result = this.service.getProductsByTable(
					new PriceTableProductFilterDto(idTabelaPreco, nome, referencia, idTipoProduto, idFornecedor, minPeso, maxPeso, minPreco, maxPreco),
					pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping("/save-prices")
	public ResponseEntity<Object> savePrices(@RequestBody List<PriceTableProductDto> dtos, @AuthenticationPrincipal Usuario usuario) {
		try {
			this.service.savePrices(dtos, usuario);
			return ResponseEntity.ok("Preços registrados");
		} catch (NoSuchElementException e) {
			String mensagem = "Alguma tabela de preços ou produto não encontrado na base";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		} catch (RuntimeException e) {
			String mensagem = "Erro ao salvar " + ENTITY.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

}
