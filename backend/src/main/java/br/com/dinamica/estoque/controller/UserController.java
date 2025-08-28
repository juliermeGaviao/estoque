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
import br.com.dinamica.estoque.service.ProfileService;
import br.com.dinamica.estoque.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

	private static final String USER_NOT_FOUND = "Usuário não encontrado: ";

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
			log.error(USER_NOT_FOUND + id, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(USER_NOT_FOUND + id);
		}
	}

	@GetMapping("/list")
	public ResponseEntity<Object> listUsers(
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
			log.error("Erro ao listar usuários", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao recuperar lista de usuários.");
		}
	}

	@PostMapping
	public ResponseEntity<Object> save(@RequestBody UserRequestDTO dto) {
		try {
			return ResponseEntity.ok(this.userService.save(dto));
		} catch (NoSuchElementException e) {
			log.error(USER_NOT_FOUND + dto.getId(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(USER_NOT_FOUND + dto.getId());
		} catch (DataIntegrityViolationException | ConstraintViolationException e) {
			log.error("Existe outro usuário com o e-mail " + dto.getEmail(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Existe outro usuário com o e-mail " + dto.getEmail());
		} catch (RuntimeException e) {
			log.error("Erro ao salvar usuário", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar usuário.");
		}
	}

	@PostMapping("/password")
	public ResponseEntity<Object> password(@RequestBody UserRequestDTO dto) {
		try {
			return ResponseEntity.ok(this.userService.changePassword(dto));
		} catch (NoSuchElementException e) {
			log.error(USER_NOT_FOUND + dto.getId(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(USER_NOT_FOUND + dto.getId());
		} catch (RuntimeException e) {
			log.error("Erro ao trocar a senha do usuário", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao trocar a senha do usuário.");
		}
	}

	@DeleteMapping
	public ResponseEntity<Object> delete(@RequestParam Long id) {
		try {
			this.userService.delete(id);

			return ResponseEntity.ok().build();
		} catch (NoSuchElementException e) {
			log.error(USER_NOT_FOUND + id, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(USER_NOT_FOUND + id);
		} catch (RuntimeException e) {
			log.error("Erro ao salvar usuário", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao remover usuário.");
		}
	}

}
