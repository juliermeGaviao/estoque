package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.PontoVenda;

public interface PontoVendaRepository extends JpaRepository<PontoVenda, Long>, JpaSpecificationExecutor<PontoVenda> {

}
