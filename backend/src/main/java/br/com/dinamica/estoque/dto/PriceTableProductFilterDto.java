package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class PriceTableProductFilterDto {
	private Long idTabelaPreco;
	private String nome;
	private String referencia;
	private Long idTipoProduto;
	private Long idFornecedor;
	private Integer minPeso;
	private Integer maxPeso;
	private Integer minPreco;
	private Integer maxPreco;
}
