package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeDto {

    private Long id;

    private ProductTypeDto pai;

    private String nome;

    private Date dataCriacao;

    private Date dataAlteracao;

}
