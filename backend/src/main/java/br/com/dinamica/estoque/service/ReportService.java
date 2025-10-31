package br.com.dinamica.estoque.service;

import java.util.List;

import br.com.dinamica.estoque.dto.ReportGroupDTO;

public interface ReportService {

	List<ReportGroupDTO> getSaleReport(Integer frequency);

	List<ReportGroupDTO> getSalesmanReport(Integer frequency);

}
