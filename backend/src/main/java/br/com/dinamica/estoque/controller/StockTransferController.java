package br.com.dinamica.estoque.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.dto.StockTransferProductDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.service.StockTransferService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/stock-transfer")
@Slf4j
public class StockTransferController {

	private static final String ENTITY = "Transferência de estoque";
	private static final String ENTITIES = "Transferências de estoque";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private StockTransferService service;

	private StockService stockService;

	public StockTransferController(StockTransferService service, StockService stockService) {
		this.service = service;
		this.stockService = stockService;
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
			@RequestParam(required = false) Long idPontoVendaOrigem,
			@RequestParam(required = false) Long idPontoVendaDestino,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate minDataTransferencia,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate maxDataTransferencia,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,desc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<StockTransferDto> result = this.service.list(idPontoVendaOrigem, idPontoVendaDestino, minDataTransferencia, maxDataTransferencia, pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody StockTransferDto dto, @AuthenticationPrincipal Usuario usuario) {
		try {
			StockTransferDto result = this.service.save(dto, usuario);

			dto.getEstoque().forEach(estoque -> {
				this.stockService.transferStock(estoque.getIdProduto(), result.getPontoVendaOrigem().getId(), result.getPontoVendaDestino().getId(), result.getId(), estoque.getQuantidade(), usuario);
			});

			return ResponseEntity.ok(result);
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

	@GetMapping("/list-products")
	public ResponseEntity<Object> listProducts(@RequestParam(required = true) Long idPontoVendaOrigem, @RequestParam(required = true) Long idPontoVendaDestingo) {
		try {
			List<StockDto> estoques = this.stockService.getStockBySalePoint(idPontoVendaOrigem);

			return ResponseEntity.ok(estoques.stream().map(dto -> {
				StockTransferProductDto result = new StockTransferProductDto(dto.getProduto());

				result.setEstoque(dto.getSaldo());
				result.setEstoqueDestino(this.stockService.getStockSalePoint(result.getId(), idPontoVendaDestingo));

				return result;
			}).toList());
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar produtos.";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@GetMapping("/list-sale-point-products")
	public ResponseEntity<Object> listPurchaseOrderProducts(@RequestParam(required = true) Long idTransferenciaEstoque) {
		try {
			List<StockDto> estoques = this.stockService.getStockTransferProducts(idTransferenciaEstoque);

			return ResponseEntity.ok(estoques.stream().map(estoque -> {
				ProductDto result = estoque.getProduto();

				result.setEstoque(estoque.getQuantidade());

				return result;
			}).toList());
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar produtos.";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

}
