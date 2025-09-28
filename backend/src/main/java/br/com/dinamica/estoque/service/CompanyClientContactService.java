package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.CompanyClientContactDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface CompanyClientContactService {

	CompanyClientContactDto get(Long id);

	Page<CompanyClientContactDto> list(Long idEmpresa, Pageable pageable);

	CompanyClientContactDto save(CompanyClientContactDto dto, Usuario usuario);

	void delete(Long id);

}
