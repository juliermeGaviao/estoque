package br.com.dinamica.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.ItemVenda;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long>, JpaSpecificationExecutor<ItemVenda> {

	@Transactional
    Long deleteByVenda_Id(Long vendaId);

	@Query(value = """
            SELECT tpp.produto.id, tpp.id, tpp.produto.nome, tpp.produto.referencia, tpp.preco,
            COALESCE(
                 (SELECT e.saldo
                 FROM Estoque e
                 WHERE e.produto.id = tpp.produto.id AND e.pontoVenda.id = :idPontoVenda AND e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = tpp.produto.id AND s.pontoVenda.id = :idPontoVenda)), 
                 0)
            FROM TabelaPrecoProduto tpp
            WHERE tpp.tabela.id = :idTabelaPreco
            ORDER BY tpp.produto.nome
        """)
	List<Object[]> getItensByPriceTable(@Param("idTabelaPreco") Long idTabelaPreco, @Param("idPontoVenda") Long idPontoVenda);

	@Query(value = """
            SELECT iv.id, iv.venda.id, tpp.produto.id, tpp.id, tpp.produto.nome, tpp.produto.referencia, iv.quantidade, tpp.preco, iv.total,
            COALESCE(
                 (SELECT e.saldo
                 FROM Estoque e
                 WHERE e.produto.id = tpp.produto.id AND e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = tpp.produto.id)), 
                 0)
            FROM TabelaPrecoProduto tpp
            JOIN Venda v ON v.tabela.id = tpp.tabela.id
            LEFT JOIN ItemVenda iv ON iv.tabelaPrecoProduto = tpp AND iv.venda.id = v.id
            WHERE v.id = :idVenda
            ORDER BY tpp.produto.nome
        """)
	List<Object[]> getItensBySale(@Param("idVenda") Long idVenda);

	@Query("FROM ItemVenda iv WHERE iv.venda.id = :idVenda AND iv.id NOT IN :ids")
	List<ItemVenda> getItensByVendaIdAndNotInIds(@Param("idVenda") Long idVenda, @Param("ids") List<Long> ids);

}
