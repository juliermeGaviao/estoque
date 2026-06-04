package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferDto {

	private Long id;

    private SalePointDto pontoVendaOrigem;

    private SalePointDto pontoVendaDestino;

    private Date dataTransferencia;

}
