package br.com.dinamica.estoque.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.UserSalePointDto;

public interface UserSalePointService {

	UserSalePointDto get(Long id);

	Page<UserSalePointDto> list(Long idUsuario, Long idPontoVenda, Pageable pageable);

	UserSalePointDto save(UserSalePointDto dto);

	void delete(Long id);

	void saveSalePoints(List<UserSalePointDto> dtos);
}
