package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemDto {

    private Long id;

    private SaleDto venda;

    private PriceTableProductDto tabelaPrecoProduto;

    private Integer quantidade;

    private BigDecimal precoUnitario;

}
