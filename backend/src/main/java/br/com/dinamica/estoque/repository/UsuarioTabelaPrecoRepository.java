package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;

public interface UsuarioTabelaPrecoRepository extends JpaRepository<UsuarioTabelaPreco, Long>, JpaSpecificationExecutor<UsuarioTabelaPreco> {

	@Transactional
	Long deleteByTabela_Id(Long tabelaId);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from UsuarioTabelaPreco u where u.vendedor.id = :vendedorId")
	Integer deleteByVendedor(@Param("vendedorId") Long vendedorId);

}
