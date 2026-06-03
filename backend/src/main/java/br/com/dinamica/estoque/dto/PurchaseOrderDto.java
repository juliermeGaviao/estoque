package br.com.dinamica.estoque.dto;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDto {

	private Long id;

    private String numeroPedido;

    private ProviderDto fornecedor;

    private Date dataPedido;

    private List<StockProductDto> estoque;

}
