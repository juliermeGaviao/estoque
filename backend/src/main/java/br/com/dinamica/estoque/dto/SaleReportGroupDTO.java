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
public class SaleReportGroupDTO {

	private String grupo;
	private List<SaleReportGroupDTO> subGrupos;
	private SaleMeasureDTO indicadores;

	public SaleReportGroupDTO(String grupo) {
		this.grupo = grupo;
	}

}
