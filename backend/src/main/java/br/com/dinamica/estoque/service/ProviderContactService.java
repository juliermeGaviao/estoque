package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ProviderContactService {

	ProviderContactDto get(Long id);

	Page<ProviderContactDto> list(Integer idFornecedor, Pageable pageable);

	ProviderContactDto save(ProviderContactDto dto, Usuario usuario);

	void delete(Long id);

}
