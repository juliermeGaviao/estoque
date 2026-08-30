package br.com.dinamica.estoque.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import br.com.dinamica.estoque.util.SafeDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {

	@JsonProperty("nome")
	private String nome;

	@JsonProperty("numero-cracha")
	private String cracha;

    @JsonProperty("data-aniversario")
	@JsonDeserialize(using = SafeDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", timezone = "America/Sao_Paulo")
    private LocalDate dataAniversario;

	@JsonProperty("limite-gasto")
	private Long limite;

}
