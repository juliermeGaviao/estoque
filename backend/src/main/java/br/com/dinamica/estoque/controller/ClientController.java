package br.com.dinamica.estoque.controller;

import java.util.Date;
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

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.PageResponse;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ClientService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/client")
@Slf4j
public class ClientController {

	private static final String ENTITY = "Empresa Cliente";
	private static final String ENTITIES = "Empresas Clientes";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private ClientService service;

	public ClientController(ClientService service) {
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

	@GetMapping("/find-all")
	public ResponseEntity<Object> list() {
		try {
			List<CommonClientDto> result = this.service.findAll();

			return ResponseEntity.ok(result);
		} catch (RuntimeException e) {
			String mensagem = "Erro ao buscar lista completa de " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@GetMapping("/list-companies")
	public ResponseEntity<Object> list(
			@RequestParam(required = false) String razaoSocial,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String cnpj,
			@RequestParam(required = false) String fone,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<ClientDto> result = this.service.list(razaoSocial, nome, cnpj, fone, pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@GetMapping("/list-people")
	public ResponseEntity<Object> list(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long idEmpresa,
			@RequestParam(required = false) String fone,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") Date minAniversario,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") Date maxAniversario,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<ClientDto> result = this.service.list(nome, idEmpresa, fone, minAniversario, maxAniversario, pageable);

			return ResponseEntity.ok(PageResponse.from(result));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody ClientDto dto, @AuthenticationPrincipal Usuario usuario) {
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

}
