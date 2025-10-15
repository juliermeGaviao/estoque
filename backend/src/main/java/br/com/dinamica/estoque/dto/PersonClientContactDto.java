package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonClientContactDto {

    private Long id;

    private ClientDto cliente;

    private String whatsapp;

    private String email;

    private String observacoes;

}
