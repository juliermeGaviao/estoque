package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.ContatoClienteEmpresa;

public interface ContatoClienteEmpresaRepository extends JpaRepository<ContatoClienteEmpresa, Long>, JpaSpecificationExecutor<ContatoClienteEmpresa> {

	@Transactional
    Long deleteByCliente_Id(Long clienteId);

}
