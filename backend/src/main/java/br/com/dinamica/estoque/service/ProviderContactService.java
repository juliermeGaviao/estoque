package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProviderContactDto;

public interface ProviderContactService {

	ProviderContactDto get(Long id);

	Page<ProviderContactDto> list(Integer idFornecedor, Pageable pageable);

	ProviderContactDto save(ProviderContactDto dto);

	void delete(Long id);

}
