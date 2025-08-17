package br.com.dinamica.estoque.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.service.ProfileService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

	private ProfileService profileService;

	public UserController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/profiles")
	public ResponseEntity<Object> getProfiles() {
		try {
			return ResponseEntity.ok(this.profileService.getProfiles());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

}
