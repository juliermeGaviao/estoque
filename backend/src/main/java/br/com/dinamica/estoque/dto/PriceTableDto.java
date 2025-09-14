package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceTableDto {

	private Long id;

    private String nome;

    private Date dataCriacao;

    private Date dataAlteracao;

}
