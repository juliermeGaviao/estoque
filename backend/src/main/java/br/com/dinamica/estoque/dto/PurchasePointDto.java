package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePointDto {

	private Long id;

    private String numeroPedido;

    private ProviderDto fornecedor;

    private Date dataPedido;

}
