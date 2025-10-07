package br.com.dinamica.estoque.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.dto.PageResponse;
import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.service.UserPriceTableService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user-price-table")
@Slf4j
public class UserPriceTableController {

	private static final String ENTITY = "Vendedor da Tabela de preços";
	private static final String ENTITIES = "Vendedor da Tabelas de preços";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private UserPriceTableService service;

	public UserPriceTableController(UserPriceTableService service) {
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
			@RequestParam(required = false) Long idVendedor,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<UserPriceTableDto> result = this.service.list(idTabelaPreco, idVendedor, pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody UserPriceTableDto dto) {
		try {
			return ResponseEntity.ok(this.service.save(dto));
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

	@PostMapping("/save-tables")
	public ResponseEntity<Object> savePrices(@RequestBody List<UserPriceTableDto> dtos) {
		try {
			this.service.saveTables(dtos);
			return ResponseEntity.ok("Tabelas registradas");
		} catch (NoSuchElementException e) {
			String mensagem = "Alguma tabela de preços ou usuário não encontrado na base";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		} catch (RuntimeException e) {
			String mensagem = "Erro ao salvar " + ENTITY.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

}
