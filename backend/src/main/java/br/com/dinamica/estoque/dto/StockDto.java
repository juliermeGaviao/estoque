package br.com.dinamica.estoque.dto;

import br.com.dinamica.estoque.entity.TipoOperacao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {

	private Long id;

    private ProductDto produto;

    private SalePointDto pontoVenda;

    private PurchasePointDto pedidoCompra;

    private SaleDto venda;

    private StockTransferDto transferenciaEstoque;

    private Integer quantidade;

    private TipoOperacao tipoOperacao;

    private Integer saldo;

}
