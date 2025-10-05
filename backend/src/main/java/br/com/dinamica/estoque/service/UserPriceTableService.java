package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface UserPriceTableService {

	UserPriceTableDto get(Long id);

	Page<UserPriceTableDto> list(Long idTabelaPreco, Long idVendedor, Pageable pageable);

	UserPriceTableDto save(UserPriceTableDto dto, Usuario usuario);

	void delete(Long id);

}
