package br.com.dinamica.estoque.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.PersonFilterDto;
import br.com.dinamica.estoque.dto.ResultadoCargaEmpregadosDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ClientService {

	ClientDto get(Long id);

	List<CommonClientDto> findAll();

	Page<ClientDto> list(String razaoSocial, String nome, String cnpj, String fone, Pageable pageable);

	Page<ClientDto> list(PersonFilterDto filter, Pageable pageable);

	ClientDto save(ClientDto dto, Usuario usuario);

	void delete(Long id);

	ResultadoCargaEmpregadosDto loadEmployees(Long idEmpresa, MultipartFile file, Usuario usuario) throws IOException;

}
