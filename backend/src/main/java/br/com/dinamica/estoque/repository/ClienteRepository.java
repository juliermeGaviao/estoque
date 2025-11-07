package br.com.dinamica.estoque.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ClientePessoa;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

	@Query("from ClientePessoa p where p.empresa.id = :idEmpresa and p.cracha = :cracha")
	Optional<ClientePessoa> getEmployee(@Param("idEmpresa") Long idEmpresa, @Param("cracha") String cracha);

}
