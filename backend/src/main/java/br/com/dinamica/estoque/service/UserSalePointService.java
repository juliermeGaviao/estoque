package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface UserSalePointService {

	UserSalePointDto get(Long id);

	Page<UserSalePointDto> list(Long idUsuario, Long idPontoVenda, Pageable pageable);

	UserSalePointDto save(UserSalePointDto dto, Usuario usuario);

	void delete(Long id);

}
