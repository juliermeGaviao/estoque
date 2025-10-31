package br.com.dinamica.estoque.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReportGroupDTO {

	private String grupo;
	private List<ReportGroupDTO> subGrupos;
	private ReportMeasureDTO indicadores;

	public ReportGroupDTO(String grupo) {
		this.grupo = grupo;
	}

}
