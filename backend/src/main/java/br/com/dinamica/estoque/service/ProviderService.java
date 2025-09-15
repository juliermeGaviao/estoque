package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProviderDto;

public interface ProviderService {

	ProviderDto get(Long id);

	Page<ProviderDto> list(Pageable pageable);

	ProviderDto save(ProviderDto dto);

	void delete(Long id);

}
