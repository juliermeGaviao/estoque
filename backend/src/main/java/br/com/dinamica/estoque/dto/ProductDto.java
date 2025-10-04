package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

	private Long id;

    private ProductTypeDto tipoProduto;

    private ProviderDto fornecedor;

    private String referencia;

    private String nome;

    private Integer peso;

    private Boolean ativo;

}
