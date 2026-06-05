package br.com.dinamica.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private LocalDate dataAniversario;

    private String endereco;

    private String bairro;

    private String cep;

    private String cidade;

    private String uf;

    private BigDecimal limite;

    private String cracha;

}
