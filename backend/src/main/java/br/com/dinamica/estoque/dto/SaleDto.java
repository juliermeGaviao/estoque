package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleDto {

    private Long id;

    private UserDto usuario;

    private PriceTableProductDto tabelaPrecoProduto;

    private Integer quantidade;

    private Float desconto;

    private String observacoes;

}
