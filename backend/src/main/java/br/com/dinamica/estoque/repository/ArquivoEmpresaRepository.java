package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.ArquivoEmpresa;

public interface ArquivoEmpresaRepository extends JpaRepository<ArquivoEmpresa, Long>, JpaSpecificationExecutor<ArquivoEmpresa> {

}
