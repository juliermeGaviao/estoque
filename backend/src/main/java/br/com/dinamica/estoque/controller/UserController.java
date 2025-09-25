package br.com.dinamica.estoque.controller;

import java.util.NoSuchElementException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
import br.com.dinamica.estoque.dto.UserListDto;
import br.com.dinamica.estoque.dto.UserRequestDTO;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProfileService;
import br.com.dinamica.estoque.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

	private static final String ENTITY = "Usuário";
	private static final String ENTITIES = "Usuários";
	private static final String NOT_FOUND = ENTITY + " não encontrado: ";

	private ProfileService profileService;

	private UserService userService;

	public UserController(ProfileService profileService, UserService userService) {
		this.profileService = profileService;
		this.userService = userService;
	}

	@GetMapping("/profiles")
	public ResponseEntity<Object> getProfiles() {
		try {
			return ResponseEntity.ok(this.profileService.getProfiles());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao recuperar lista de perfis.");
		}
	}

	@GetMapping("/get")
	public ResponseEntity<Object> getUser(@RequestParam Long id) {
		try {
			return ResponseEntity.ok(this.userService.getUser(id));
		} catch (NoSuchElementException e) {
			String mensagem = NOT_FOUND + id;
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		}
	}

	@GetMapping("/list")
	public ResponseEntity<Object> list(
			@RequestParam(required = false) String email,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id,asc") String[] sort) {
		try {
			String sortField = sort[0];
			String sortDirection = sort.length > 1 ? sort[1] : "asc";

			Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

			Page<UserListDto> usuarios = this.userService.list(email, pageable);

			return ResponseEntity.ok(PageResponse.from(usuarios));
		} catch (RuntimeException e) {
			String mensagem = "Erro ao listar " + ENTITIES.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody UserRequestDTO dto, @AuthenticationPrincipal Usuario usuario) {
		try {
			return ResponseEntity.ok(this.userService.save(dto, usuario));
		} catch (NoSuchElementException e) {
			String mensagem = NOT_FOUND + dto.getId();
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagem);
		} catch (DataIntegrityViolationException | ConstraintViolationException e) {
			log.error("Existe outro usuário com o e-mail " + dto.getEmail(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Existe outro usuário com o e-mail " + dto.getEmail());
		} catch (RuntimeException e) {
			String mensagem = "Erro ao salvar " + ENTITY.toLowerCase() + ".";
			log.error(mensagem, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mensagem);
		}
	}

	@PostMapping("/password")
	public ResponseEntity<Object> password(@RequestBody UserRequestDTO dto) {
		try {
			return ResponseEntity.ok(this.userService.changePassword(dto));
		} catch (NoSuchElementException e) {
			log.error(NOT_FOUND + dto.getId(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NOT_FOUND + dto.getId());
		} catch (RuntimeException e) {
			log.error("Erro ao trocar a senha do usuário.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao trocar a senha do usuário.");
		}
	}

	@DeleteMapping
	public ResponseEntity<Object> delete(@RequestParam Long id) {
		try {
			this.userService.delete(id);

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
