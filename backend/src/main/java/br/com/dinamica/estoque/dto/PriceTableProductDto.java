package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceTableProductDto {

	private Long id;

    private ProductDto produto;

    private PriceTableDto tabela;

    private BigDecimal preco;

}
