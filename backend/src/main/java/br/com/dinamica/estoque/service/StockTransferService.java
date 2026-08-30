package br.com.dinamica.estoque.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface StockTransferService {

	StockTransferDto get(Long id);

	Page<StockTransferDto> list(Long idPontoVendaOrigem, Long idPontoVendaDestino, LocalDate minDataTransferencia, LocalDate maxDataTransferencia, Pageable pageable);

	StockTransferDto save(StockTransferDto dto, Usuario usuario);

	void delete(Long id);

}
