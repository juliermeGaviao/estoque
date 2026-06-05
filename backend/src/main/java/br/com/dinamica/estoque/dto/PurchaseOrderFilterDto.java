package br.com.dinamica.estoque.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderFilterDto {

    private String numeroPedido;

    private Long idFornecedor;

    private LocalDate minDataPedido;

    private LocalDate maxDataPedido;

}
