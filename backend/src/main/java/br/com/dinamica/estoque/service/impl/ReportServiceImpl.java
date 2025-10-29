package br.com.dinamica.estoque.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.SaleMeasureDTO;
import br.com.dinamica.estoque.dto.SaleReportGroupDTO;
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
	public List<SaleReportGroupDTO> getSaleReport(Integer frequency) {
		List<Object[]> records = new ArrayList<>();

		if (frequency.equals(1)) {
			records = this.vendaRepository.findRelatorioVendaDiario();
		} else if (frequency.equals(2)) {
			records = this.vendaRepository.findRelatorioVendaSemanal();
		} else if (frequency.equals(3)) {
			records = this.vendaRepository.findRelatorioVendaMensal();
		}

		List<SaleReportGroupDTO> result = new ArrayList<>();

		records.forEach(linha -> {
			String periodo = frequency.equals(3) ? (String) linha[0] : this.dateFormatter.format((Date) linha[0]);
			int index = Collections.binarySearch(result, new SaleReportGroupDTO(periodo));
			SaleReportGroupDTO dto = null;

			if (index >= 0) {
				dto = result.get(index);
			} else {
				dto = new SaleReportGroupDTO(periodo, null, new ArrayList<>());
				result.add(dto);
			}

			dto.getIndicadores().add(new SaleMeasureDTO(((Number) linha[1]).longValue(), (BigDecimal) linha[2], (BigDecimal) linha[3]));
		});

		return result;
	}

	@Override
	public List<SaleReportGroupDTO> getSalesmanReport(Integer frequency) {
		List<Object[]> records = new ArrayList<>();

		if (frequency.equals(1)) {
			records = this.vendaRepository.findRelatorioVendedorDiario();
		} else if (frequency.equals(2)) {
			records = this.vendaRepository.findRelatorioVendedorSemanal();
		} else if (frequency.equals(3)) {
			records = this.vendaRepository.findRelatorioVendedorMensal();
		}

		List<SaleReportGroupDTO> result = new ArrayList<>();

		records.forEach(linha -> {
			String periodo = frequency.equals(3) ? (String) linha[0] : this.dateFormatter.format((Date) linha[0]);
			String email = (String) linha[1];
			SaleReportGroupDTO grupo = null;
			int index = Collections.binarySearch(result, new SaleReportGroupDTO(periodo));

			if (index >= 0) {
				grupo = result.get(index);
			} else {
				grupo = new SaleReportGroupDTO(periodo, new ArrayList<>(), null);
				result.add(grupo);
			}

			SaleReportGroupDTO dto = null;

			index = Collections.binarySearch(grupo.getSubGrupos(), new SaleReportGroupDTO(email));
			if (index >= 0) {
				dto = grupo.getSubGrupos().get(index);
			} else {
				dto = new SaleReportGroupDTO(email, null, new ArrayList<>());
				grupo.getSubGrupos().add(dto);
			}

			dto.getIndicadores().add(new SaleMeasureDTO(((Number) linha[2]).longValue(), (BigDecimal) linha[3], (BigDecimal) linha[4]));
		});

		return result;
	}

}
