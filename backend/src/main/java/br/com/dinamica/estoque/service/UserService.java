package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserListDto;
import br.com.dinamica.estoque.dto.UserRequestDTO;

public interface UserService {

	UserDto getUser(Long id);

	Page<UserListDto> list(String email, Pageable pageable);

	UserDto save(UserRequestDTO dto);

	UserDto changePassword(UserRequestDTO dto);

	void delete(Long id);

}
