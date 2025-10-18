package br.com.dinamica.estoque.service;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ClientService {

	ClientDto get(Long id);

	List<CommonClientDto> findAll();

	Page<ClientDto> list(String razaoSocial, String nome, String cnpj, String fone, Pageable pageable);

	Page<ClientDto> list(String nome, Long idEmpresa, String fone, Date minAniversario, Date maxAniversario, Pageable pageable);

	ClientDto save(ClientDto dto, Usuario usuario);

	void delete(Long id);

}
