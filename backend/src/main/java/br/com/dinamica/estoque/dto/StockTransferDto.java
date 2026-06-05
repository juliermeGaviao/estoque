package br.com.dinamica.estoque.dto;

import java.time.LocalDate;

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

    private LocalDate dataTransferencia;

}
