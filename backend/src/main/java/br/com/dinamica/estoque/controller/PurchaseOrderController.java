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
import br.com.dinamica.estoque.dto.ProductFilterDto;
import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.service.PurchaseOrderService;
import br.com.dinamica.estoque.service.StockService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/purchase-order")
@Slf4j
public class PurchaseOrderController {

	private static final Long LOGISTIC_HUB = 1L;

	private static final String ENTITY = "Pedido de compra";
	private static final String ENTITIES = "Pedidos de venda";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private PurchaseOrderService service;

	private ProductService productService;

	private StockService stockService;

	public PurchaseOrderController(PurchaseOrderService service, ProductService productService, StockService stockService) {
		this.service = service;
		this.productService = productService;
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
			@RequestParam(required = false) String numeroPedido,
			@RequestParam(required = false) Long idFornecedor,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate minDataPedido,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate maxDataPedido,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,desc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<PurchaseOrderDto> result = this.service.list(new PurchaseOrderFilterDto(numeroPedido, idFornecedor, minDataPedido, maxDataPedido), pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody PurchaseOrderDto dto, @AuthenticationPrincipal Usuario usuario) {
		try {
			PurchaseOrderDto result = this.service.save(dto, usuario);

			dto.getEstoque().forEach(estoque -> {
				this.stockService.addStock(estoque.getIdProduto(), LOGISTIC_HUB, result.getId(), estoque.getQuantidade(), usuario);
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
	public ResponseEntity<Object> listProducts(@RequestParam(required = true) Long idFornecedor) {
		try {
			ProductFilterDto productFilter = new ProductFilterDto();

			productFilter.setIdFornecedor(idFornecedor);

			Pageable pageable = PageRequest.of(0, 10000, Sort.by("nome").ascending());

			Page<ProductDto> produtos = this.productService.list(productFilter, pageable);

			return ResponseEntity.ok(produtos.stream().filter(dto -> idFornecedor.equals(dto.getFornecedor().getId())).map(result -> {
				result.setEstoque(this.stockService.getStock(result.getId()));

				return result;
			}).toList());
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar produtos.";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@GetMapping("/list-purchase-order-products")
	public ResponseEntity<Object> listPurchaseOrderProducts(@RequestParam(required = true) Long idPedidoCompra) {
		try {
			List<StockDto> estoques = this.stockService.getPurchaseOrderProducts(idPedidoCompra);

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
