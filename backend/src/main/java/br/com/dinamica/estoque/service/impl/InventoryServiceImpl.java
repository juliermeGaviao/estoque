package br.com.dinamica.estoque.service.impl;

import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.TipoOperacao;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.EstoqueRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.service.InventoryService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class InventoryServiceImpl implements InventoryService {

	private EstoqueRepository repository;

	private ProdutoRepository produtoRepository;

	public InventoryServiceImpl(EstoqueRepository repository, ProdutoRepository produtoRepository) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
	}

	@Override
	public Estoque addAmount(Long idProduto, Integer amount, Usuario usuario) {
		if (amount == null || amount.compareTo(0) == 0) {
			return null;
		}

		Estoque current = this.repository.getInventoryByProduct(idProduto);
		Estoque inventory = new Estoque();
		
		inventory.setProduto(this.produtoRepository.findById(idProduto).orElseThrow());
		inventory.setQuantidade(Math.abs(amount));
		inventory.setTipoOperacao(amount.compareTo(0) > 0 ? TipoOperacao.C : TipoOperacao.D);
		inventory.setSaldo((current != null && current.getSaldo() != null ? current.getSaldo() : 0) + amount);
		inventory.setUsuario(usuario);
		inventory.setDataCriacao(DateUtil.now());

		return this.repository.save(inventory);
	}

	@Override
	public Integer getAmount(Long idProduto) {
		Estoque result = this.repository.getInventoryByProduct(idProduto);

		return result != null ? result.getSaldo() : 0;
	}
}
