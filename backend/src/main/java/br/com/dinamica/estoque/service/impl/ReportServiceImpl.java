package br.com.dinamica.estoque.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ReportGroupDTO;
import br.com.dinamica.estoque.dto.ReportMeasureDTO;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

	private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

	private VendaRepository vendaRepository;

	public ReportServiceImpl(VendaRepository vendaRepository) {
		this.vendaRepository = vendaRepository;
	}

	@Override
	public List<ReportGroupDTO> getSaleReport(Integer frequency) {
		List<Object[]> records = new ArrayList<>();

		if (frequency.equals(1)) {
			records = this.vendaRepository.findRelatorioVendaDiario();
		} else if (frequency.equals(2)) {
			records = this.vendaRepository.findRelatorioVendaSemanal();
		} else if (frequency.equals(3)) {
			records = this.vendaRepository.findRelatorioVendaMensal();
		}

		List<ReportGroupDTO> result = new ArrayList<>();

		for (Object[] linha : records) {
			String periodo = frequency.equals(3) ? (String) linha[0] : this.dateFormatter.format((Date) linha[0]);
			ReportGroupDTO dto = result.stream().filter(item -> periodo.equals(item.getGrupo())).findFirst().orElse(null);

			if (dto == null) {
				dto = new ReportGroupDTO(periodo, null, null);
				result.add(dto);
			}

			dto.setIndicadores(new ReportMeasureDTO(((Number) linha[1]).longValue(), (BigDecimal) linha[2], (BigDecimal) linha[3]));
		}

		return result;
	}

	@Override
	public List<ReportGroupDTO> getSalesmanReport(Integer frequency) {
		List<Object[]> records = new ArrayList<>();

		if (frequency.equals(1)) {
			records = this.vendaRepository.findRelatorioVendedorDiario();
		} else if (frequency.equals(2)) {
			records = this.vendaRepository.findRelatorioVendedorSemanal();
		} else if (frequency.equals(3)) {
			records = this.vendaRepository.findRelatorioVendedorMensal();
		}

		List<ReportGroupDTO> result = new ArrayList<>();

		for (Object[] linha : records) {
			String periodo = frequency.equals(3) ? (String) linha[0] : this.dateFormatter.format((Date) linha[0]);
			String email = (String) linha[1];
			ReportGroupDTO grupo = result.stream().filter(item -> periodo.equals(item.getGrupo())).findFirst().orElse(null);

			if (grupo == null) {
				grupo = new ReportGroupDTO(periodo, new ArrayList<>(), null);
				result.add(grupo);
			}
			
			ReportMeasureDTO measureDto = new ReportMeasureDTO(((Number) linha[2]).longValue(), (BigDecimal) linha[3], (BigDecimal) linha[4]);
			
			grupo.getSubGrupos().add(new ReportGroupDTO(email, null, measureDto));
		}

		return result;
	}

}
