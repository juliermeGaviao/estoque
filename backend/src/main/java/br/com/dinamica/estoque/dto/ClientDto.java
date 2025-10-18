package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientDto extends CommonClientDto {

	private ClientDto empresa;

    private String razaoSocial;

    private String cnpj;

    private String fone;

    private Date dataAniversario;

    private String endereco;

    private String bairro;

    private String cep;

    private String cidade;

    private String uf;

}
