package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.PersonClientContactDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface PersonClientContactService {

	PersonClientContactDto get(Long id);

	Page<PersonClientContactDto> list(Pageable pageable);

	PersonClientContactDto save(PersonClientContactDto dto, Usuario usuario);

	void delete(Long id);

}
