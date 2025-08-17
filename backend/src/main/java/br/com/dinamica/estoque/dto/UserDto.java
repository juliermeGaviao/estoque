package br.com.dinamica.estoque.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;

    private String email;

    private Date dataCriacao;

    private Date dataAlteracao;

    private List<ProfileDto> perfis = new ArrayList<>();

}
