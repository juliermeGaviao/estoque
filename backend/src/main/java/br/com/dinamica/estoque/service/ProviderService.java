package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ProviderService {

	ProviderDto get(Long id);

	Page<ProviderDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable);

	ProviderDto save(ProviderDto dto, Usuario usuario);

	void delete(Long id);

}
