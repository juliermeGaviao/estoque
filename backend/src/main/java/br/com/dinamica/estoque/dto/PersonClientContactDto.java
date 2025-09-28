package br.com.dinamica.estoque.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonClientContactDto {

    private Long id;

    private PersonClientDto clientePessoa;

    private String whatsapp;

    private String email;

    private Date dataAniversario;

    private String observacoes;

}
