package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.ClienteEmpresa;

public interface ClienteEmpresaRepository extends JpaRepository<ClienteEmpresa, Long>, JpaSpecificationExecutor<ClienteEmpresa> {

}
