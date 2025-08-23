package br.com.dinamica.estoque.service;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserListDto;

public interface UserService {

	UserDto getUser(Long id) throws NoSuchElementException;

	Page<UserListDto> list(String email, Pageable pageable);

}
