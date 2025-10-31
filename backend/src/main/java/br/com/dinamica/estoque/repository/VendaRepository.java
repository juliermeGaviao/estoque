package br.com.dinamica.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import br.com.dinamica.estoque.entity.Venda;

public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

	@Query(value = """
	        SELECT
	            DATE(v.data_alteracao) AS periodo,
	            COUNT(*) AS quantidade_vendas,
	            AVG(v.total) AS media_total,
	            SUM(v.total) AS soma_total
	        FROM venda v
	        WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
	        GROUP BY DATE(v.data_alteracao)
	        ORDER BY periodo
	""", nativeQuery = true)
	List<Object[]> findRelatorioVendaDiario();

	@Query(value = """
			SELECT
			    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEK(CURDATE()) - WEEK(v.data_alteracao)) WEEK), INTERVAL (7 - DAYOFWEEK(CURDATE())) DAY) AS periodo,
			    COUNT(*) AS quantidade_vendas,
			    AVG(v.total) AS media_total,
			    SUM(v.total) AS soma_total
			FROM venda v
			WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 4 WEEK)
			GROUP BY periodo
			ORDER BY periodo
			""", nativeQuery = true)
	List<Object[]> findRelatorioVendaSemanal();

	@Query(value = """
			SELECT
			    DATE_FORMAT(v.data_alteracao, '%m/%Y') AS periodo,
			    COUNT(*) AS quantidade_vendas,
			    AVG(v.total) AS media_total,
			    SUM(v.total) AS soma_total
			FROM venda v
			WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
			GROUP BY DATE_FORMAT(v.data_alteracao, '%m/%Y')
			ORDER BY periodo
			""", nativeQuery = true)
	List<Object[]> findRelatorioVendaMensal();

	@Query(value = """
            SELECT
                DATE(v.data_alteracao) AS periodo,
                u.email,
                COUNT(*) AS quantidade_vendas,
                AVG(v.total) AS media_total,
                SUM(v.total) AS soma_total
            FROM venda v JOIN usuario u ON u.id = v.id_vendedor
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(v.data_alteracao), v.id_vendedor
            ORDER BY periodo, u.email
    """, nativeQuery = true)
	List<Object[]> findRelatorioVendedorDiario();

	@Query(value = """
            SELECT
                DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEK(CURDATE()) - WEEK(v.data_alteracao)) WEEK), INTERVAL (7 - DAYOFWEEK(CURDATE())) DAY) AS periodo,
                u.email,
                COUNT(*) AS quantidade_vendas,
                AVG(v.total) AS media_total,
                SUM(v.total) AS soma_total
            FROM venda v JOIN usuario u ON u.id = v.id_vendedor
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 4 WEEK)
            GROUP BY periodo, v.id_vendedor
            ORDER BY periodo, u.email
    """, nativeQuery = true)
	List<Object[]> findRelatorioVendedorSemanal();

	@Query(value = """
            SELECT
                DATE_FORMAT(v.data_alteracao, '%m/%Y') AS periodo,
                u.email,
                COUNT(*) AS quantidade_vendas,
                AVG(v.total) AS media_total,
                SUM(v.total) AS soma_total
            FROM venda v JOIN usuario u ON u.id = v.id_vendedor
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
            GROUP BY DATE_FORMAT(v.data_alteracao, '%m/%Y'), v.id_vendedor
            ORDER BY periodo, u.email
    """, nativeQuery = true)
	List<Object[]> findRelatorioVendedorMensal();

	@Query(value = """
            SELECT
                DATE(v.data_alteracao) AS periodo,
                MIN(tp.nome) tipo_produto,
                COUNT(DISTINCT v.id) AS quantidade_vendas,
                AVG(iv.total) AS media_total,
                SUM(iv.total) AS soma_total
            FROM venda v
            JOIN item_venda iv ON iv.id_venda = v.id
            JOIN tabela_preco_produto tpp ON tpp.id = iv.id_tabela_preco_produto
            JOIN produto p ON p.id = tpp.id_produto
            JOIN tipo_produto tp ON tp.id = p.id_tipo_produto
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(v.data_alteracao), tp.id
            ORDER BY periodo, tipo_produto
    """, nativeQuery = true)
	List<Object[]> findRelatorioTipoProdutoDiario();

	@Query(value = """
            SELECT
                DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEK(CURDATE()) - WEEK(v.data_alteracao)) WEEK), INTERVAL (7 - DAYOFWEEK(CURDATE())) DAY) AS periodo,
                MIN(tp.nome) tipo_produto,
                COUNT(DISTINCT v.id) AS quantidade_vendas,
                AVG(iv.total) AS media_total,
                SUM(iv.total) AS soma_total
            FROM venda v
            JOIN item_venda iv ON iv.id_venda = v.id
            JOIN tabela_preco_produto tpp ON tpp.id = iv.id_tabela_preco_produto
            JOIN produto p ON p.id = tpp.id_produto
            JOIN tipo_produto tp ON tp.id = p.id_tipo_produto
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 4 WEEK)
            GROUP BY periodo, tp.id
            ORDER BY periodo, tipo_produto
    """, nativeQuery = true)
	List<Object[]> findRelatorioTipoProdutoSemanal();

	@Query(value = """
            SELECT
                DATE_FORMAT(v.data_alteracao, '%Y-%m') AS periodo,
                MIN(tp.nome) tipo_produto,
                COUNT(DISTINCT v.id) AS quantidade_vendas,
                AVG(iv.total) AS media_total,
                SUM(iv.total) AS soma_total
            FROM venda v
            JOIN item_venda iv ON iv.id_venda = v.id
            JOIN tabela_preco_produto tpp ON tpp.id = iv.id_tabela_preco_produto
            JOIN produto p ON p.id = tpp.id_produto
            JOIN tipo_produto tp ON tp.id = p.id_tipo_produto
            WHERE v.data_alteracao >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
            GROUP BY DATE_FORMAT(v.data_alteracao, '%Y-%m'), tp.id
            ORDER BY periodo, tipo_produto
    """, nativeQuery = true)
	List<Object[]> findRelatorioTipoProdutoMensal();

}
