package br.com.dinamica.estoque.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.TabelaPrecoProduto;

public interface TabelaPrecoProdutoRepository extends JpaRepository<TabelaPrecoProduto, Long>, JpaSpecificationExecutor<TabelaPrecoProduto> {

	@Transactional
	Long deleteByTabela_Id(Long tabelaId);

	@Query(value = """
			SELECT p.id, tp.nome, f.fantasia, p.referencia, p.nome, p.peso, tpp.id, tpp.preco
			FROM produto p
			JOIN tipo_produto tp ON tp.id = p.id_tipo_produto
			JOIN fornecedor f ON f.id = p.id_fornecedor
			LEFT JOIN tabela_preco_produto tpp ON p.id = tpp.id_produto AND tpp.id_tabela_preco = :idTabelaPreco
			WHERE p.ativo = 1
			""",
			countQuery = "SELECT count(*) FROM produto p WHERE p.ativo = 1",
			nativeQuery = true)
	Page<Object[]> getProductsByTable(@Param("idTabelaPreco") Long idTabelaPreco, Pageable pageable);

}
