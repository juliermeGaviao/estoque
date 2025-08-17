package br.com.dinamica.estoque.service;

import java.util.NoSuchElementException;

import br.com.dinamica.estoque.dto.UserDto;

public interface UserService {

	UserDto getUser(Long id) throws NoSuchElementException;

}
