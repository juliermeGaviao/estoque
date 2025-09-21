package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.CompanyClientDto;

public interface CompanyClientService {

	CompanyClientDto get(Long id);

	Page<CompanyClientDto> list(Pageable pageable);

	CompanyClientDto save(CompanyClientDto dto);

	void delete(Long id);

}
