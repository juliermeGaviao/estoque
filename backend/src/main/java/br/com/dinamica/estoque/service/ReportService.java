package br.com.dinamica.estoque.service;

import java.util.List;

import br.com.dinamica.estoque.dto.SaleReportGroupDTO;

public interface ReportService {

	List<SaleReportGroupDTO> getSaleReport(Integer frequency);

	List<SaleReportGroupDTO> getSalesmanReport(Integer frequency);

}
