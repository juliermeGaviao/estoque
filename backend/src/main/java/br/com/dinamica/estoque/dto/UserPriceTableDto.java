package br.com.dinamica.estoque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPriceTableDto {

	private Long id;

    private UserDto usuario;

    private PriceTableDto tabela;

}
