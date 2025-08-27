package br.com.dinamica.estoque.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequestDTO {

	private Long id;

	private String email;

	private String senha;

	private List<Long> perfis;

}
