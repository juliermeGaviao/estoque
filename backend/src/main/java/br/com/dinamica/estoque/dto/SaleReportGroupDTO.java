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
public class SaleReportGroupDTO implements Comparable<SaleReportGroupDTO> {

	private String grupo;
	private List<SaleReportGroupDTO> subGrupos;
	private List<SaleMeasureDTO> indicadores;

	public SaleReportGroupDTO(String periodo) {
		this.grupo = periodo;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SaleReportGroupDTO dto = (SaleReportGroupDTO) o;

        return grupo != null && grupo.equals(dto.grupo);
    }

    @Override
    public int hashCode() {
        return grupo != null ? grupo.hashCode() : 0;
    }

	@Override
	public int compareTo(SaleReportGroupDTO o) {
		return grupo.compareTo(o.grupo);
	}

}
