package br.com.dinamica.estoque.service;

import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.Usuario;

public interface InventoryService {

	Estoque addAmount(Long idProduto, Integer amount, Usuario usuario);

	Integer getAmount(Long idProduto);

}
