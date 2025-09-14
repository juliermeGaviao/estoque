package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDto {

    private Long id;

    private String razaoSocial;

    private String fantasia;

    private String cnpj;

    private String fone;

    private String endereco;

    private String bairro;

    private String cep;

    private String cidade;

    private String uf;

    private Date dataCriacao;

    private Date dataAlteracao;

}
