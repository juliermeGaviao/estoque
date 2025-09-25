package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyClientContactDto {

    private Long id;

    private CompanyClientDto clienteEmpresa;

    private String nome;

    private String cargo;

    private String fone;

    private String ramal;

    private String celular;

    private String email;

    private Date dataAniversario;

    private String observacoes;

}
