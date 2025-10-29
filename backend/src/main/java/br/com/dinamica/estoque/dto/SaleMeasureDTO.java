package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SaleMeasureDTO {

	private Long quantidadeVendas;
	private BigDecimal mediaTotal;
	private BigDecimal somaTotal;

}
