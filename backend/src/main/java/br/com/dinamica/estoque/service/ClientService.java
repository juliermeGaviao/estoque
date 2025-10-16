package br.com.dinamica.estoque.service;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ClientService {

	ClientDto get(Long id);

	Page<ClientDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable);

	Page<ClientDto> list(String nome, String fone, Date minAniversario, Date maxAniversario, Pageable pageable);

	ClientDto save(ClientDto dto, Usuario usuario);

	void delete(Long id);

}
