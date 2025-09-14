package br.com.dinamica.estoque.dto;

import java.util.Date;

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

    private Date dataCriacao;

    private Date dataAlteracao;

}
