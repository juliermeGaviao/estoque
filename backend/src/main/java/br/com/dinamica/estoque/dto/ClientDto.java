package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {

    private Long id;

    private String razaoSocial;

    private String nome;

    private String cnpj;

    private String fone;

    private Date dataAniversario;

    private String endereco;

    private String bairro;

    private String cep;

    private String cidade;

    private String uf;

}
