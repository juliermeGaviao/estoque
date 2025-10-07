package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.ItemVenda;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long>, JpaSpecificationExecutor<ItemVenda> {

}
