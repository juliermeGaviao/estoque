package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.dinamica.estoque.entity.Perfil;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

}
