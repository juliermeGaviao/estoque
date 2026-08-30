package br.com.dinamica.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.UsuarioPontoVenda;

public interface UsuarioPontoVendaRepository extends JpaRepository<UsuarioPontoVenda, Long>, JpaSpecificationExecutor<UsuarioPontoVenda> {

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from UsuarioPontoVenda u where u.usuario.id = :usuarioId")
	Integer deleteByUsuario(@Param("usuarioId") Long usuarioId);

	@Query("from UsuarioPontoVenda u where u.usuario.id = :usuarioId")
	List<UsuarioPontoVenda> findByUsuario(@Param("usuarioId") Long usuarioId);

}
