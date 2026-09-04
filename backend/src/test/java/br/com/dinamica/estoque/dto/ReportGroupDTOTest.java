package br.com.dinamica.estoque.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportGroupDTOTest {

    @Test
    @DisplayName("Deve testar construtor sem argumentos, setters e getters")
    void testNoArgsConstructorAndSettersGetters() {
        ReportGroupDTO dto = new ReportGroupDTO();

        String grupo = "Grupo Principal";
        List<ReportGroupDTO> subGrupos = Collections.emptyList();
        ReportMeasureDTO indicadores = new ReportMeasureDTO();

        dto.setGrupo(grupo);
        dto.setSubGrupos(subGrupos);
        dto.setIndicadores(indicadores);

        assertEquals(grupo, dto.getGrupo());
        assertEquals(subGrupos, dto.getSubGrupos());
        assertEquals(indicadores, dto.getIndicadores());
    }

    @Test
    @DisplayName("Deve testar construtor personalizado com apenas grupo")
    void testSingleArgConstructor() {
        String grupo = "Grupo A";
        ReportGroupDTO dto = new ReportGroupDTO(grupo);

        assertEquals(grupo, dto.getGrupo());
        assertNull(dto.getSubGrupos());
        assertNull(dto.getIndicadores());
    }

    @Test
    @DisplayName("Deve testar construtor com todos os argumentos")
    void testAllArgsConstructor() {
        String grupo = "Grupo B";
        List<ReportGroupDTO> subGrupos = List.of(new ReportGroupDTO("Subgrupo 1"));
        ReportMeasureDTO indicadores = new ReportMeasureDTO();

        ReportGroupDTO dto = new ReportGroupDTO(grupo, subGrupos, indicadores);

        assertEquals(grupo, dto.getGrupo());
        assertEquals(subGrupos, dto.getSubGrupos());
        assertEquals(indicadores, dto.getIndicadores());
    }
}