package br.com.dinamica.estoque.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.PriceTableProductFilterDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface PriceTableProductService {

	PriceTableProductDto get(Long id);

	Page<PriceTableProductDto> list(Long idTabelaPreco, Long idProduto, Pageable pageable);

	PriceTableProductDto save(PriceTableProductDto dto, Usuario usuario);

	void delete(Long id);

	Page<PriceTableProductDto> getProductsByTable(PriceTableProductFilterDto filter, Pageable pageable);

	void savePrices(List<PriceTableProductDto> prices, Usuario usuario);

}
