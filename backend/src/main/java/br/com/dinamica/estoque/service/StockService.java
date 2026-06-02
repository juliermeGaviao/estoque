package br.com.dinamica.estoque.service;

import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.Usuario;

public interface StockService {

	Estoque addStock(Long idProduto, Long idPontoVenda, Integer amount, Usuario usuario);

	Integer getStock(Long idProduto);

}
