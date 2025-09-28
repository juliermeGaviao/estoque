package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.PersonClientDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface PersonClientService {

	PersonClientDto get(Long id);

	Page<PersonClientDto> list(Pageable pageable);

	PersonClientDto save(PersonClientDto dto, Usuario usuario);

	void delete(Long id);

}
