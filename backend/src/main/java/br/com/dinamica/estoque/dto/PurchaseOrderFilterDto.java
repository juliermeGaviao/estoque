package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderFilterDto {

    private String numeroPedido;

    private Long idFornecedor;

    private Date minDataPedido;

    private Date maxDataPedido;

}
