package br.com.dinamica.estoque.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.dinamica.estoque.service.ReportService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/report")
@Slf4j
public class ReportController {

	private ReportService service;

	public ReportController(ReportService service) {
		this.service = service;
	}

	@GetMapping("/sale-report")
	public ResponseEntity<Object> getSaleReport(@RequestParam Integer frequency) {
		return ResponseEntity.ok(this.service.getSaleReport(frequency));
	}

	@GetMapping("/salesman-report")
	public ResponseEntity<Object> getSalesmanReport(@RequestParam Integer frequency) {
		return ResponseEntity.ok(this.service.getSalesmanReport(frequency));
	}

}
