package br.com.dinamica.estoque.dto;

import java.util.Date;

import org.springframework.lang.Nullable;

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

	@Nullable
	@JsonProperty("nome")
	private String nome;

	@Nullable
	@JsonProperty("numero-cracha")
	private String cracha;

	@Nullable
    @JsonProperty("data-aniversario")
	@JsonDeserialize(using = SafeDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", timezone = "America/Sao_Paulo")
    private Date dataAniversario;

	@Nullable
	@JsonProperty("limite-gasto")
	private Long limite;

}
