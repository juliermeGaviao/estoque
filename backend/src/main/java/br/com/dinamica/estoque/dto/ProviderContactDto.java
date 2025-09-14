package br.com.dinamica.estoque.dto;

import java.util.Date;

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

    private Date dataCriacao;

    private Date dataAlteracao;

}
