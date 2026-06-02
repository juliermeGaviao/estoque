package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleDto {

    private Long id;

    private ClientDto cliente;

    private UserDto vendedor;

    private PriceTableDto tabela;

    private BigDecimal subTotal;

    private SalePointDto pontoVenda;

    private Float desconto;

    private BigDecimal total;

    private String observacoes;

}
