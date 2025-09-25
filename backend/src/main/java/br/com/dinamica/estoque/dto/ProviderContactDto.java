package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderContactDto {

    private Long id;

    private ProviderDto fornecedor;

    private String nome;

    private String cargo;

    private String celular;

}
