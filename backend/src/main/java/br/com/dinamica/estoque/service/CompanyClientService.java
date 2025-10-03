package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.CompanyClientDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface CompanyClientService {

	CompanyClientDto get(Long id);

	Page<CompanyClientDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable);

	CompanyClientDto save(CompanyClientDto dto, Usuario usuario);

	void delete(Long id);

}
