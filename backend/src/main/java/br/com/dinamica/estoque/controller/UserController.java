package br.com.dinamica.estoque.controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.service.ProfileService;
import br.com.dinamica.estoque.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

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
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

	@GetMapping("/get")
	public ResponseEntity<Object> getUser(@RequestParam Long id) {
		try {
			return ResponseEntity.ok(this.userService.getUser(id));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário de id (" + id + ") não encontrado.");
		}
	}

}
