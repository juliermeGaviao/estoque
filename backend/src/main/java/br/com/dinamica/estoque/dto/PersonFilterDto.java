package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonFilterDto {

	private String nome;

	private Long idEmpresa;

	private String fone;

	private BigDecimal minLimite;

	private BigDecimal maxLimite;

	private LocalDate minAniversario;

	private LocalDate maxAniversario;

}
