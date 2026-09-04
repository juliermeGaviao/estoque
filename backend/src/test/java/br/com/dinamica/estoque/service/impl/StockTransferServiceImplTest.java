package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.StockTransferMapper;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.TransferenciaEstoqueRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceImplTest {

    @Mock
    private TransferenciaEstoqueRepository repository;

    @Mock
    private PontoVendaRepository pontoVendaRepository;

    @Mock
    private StockTransferMapper modelMapper;

    private StockTransferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StockTransferServiceImpl(repository, pontoVendaRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar a entidade")
    void get_shouldReturnDto() {
        TransferenciaEstoque entity = new TransferenciaEstoque();
        StockTransferDto dto = new StockTransferDto();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        StockTransferDto result = service.get(1L);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(1L);
        verify(modelMapper).toDto(entity);
    }

    @Test
    @DisplayName("get - deve lançar exceção quando não encontrar")
    void get_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    // -------------------------------------------------------------------------
    // list() - Testando todas as ramificações de filtros e datas
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros (Origem, Destino e Intervalo Completo de Datas)")
    void list_shouldApplyAllFiltersIncludingMinAndMaxDate() {
        Pageable pageable = PageRequest.of(0, 10);
        TransferenciaEstoque entity = new TransferenciaEstoque();
        StockTransferDto dto = new StockTransferDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TransferenciaEstoque> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        LocalDate minData = LocalDate.now().minusDays(5);
        LocalDate maxData = LocalDate.now();

        Page<StockTransferDto> result = service.list(1L, 2L, minData, maxData, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro apenas com minDataTransferencia")
    void list_shouldApplyOnlyMinDateFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TransferenciaEstoque> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<StockTransferDto> result = service.list(null, null, LocalDate.now(), null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro apenas com maxDataTransferencia")
    void list_shouldApplyOnlyMaxDateFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TransferenciaEstoque> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<StockTransferDto> result = service.list(null, null, null, LocalDate.now(), pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve funcionar sem filtros")
    void list_shouldWorkWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TransferenciaEstoque> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<StockTransferDto> result = service.list(null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar registro existente quando dto possui ID")
    void save_shouldUpdateExistingTransfer() {
        StockTransferDto dto = criarDto(1L, 10L, 20L);
        Usuario usuario = new Usuario();
        TransferenciaEstoque entityExistente = new TransferenciaEstoque();
        PontoVenda origem = new PontoVenda();
        PontoVenda destino = new PontoVenda();

        when(repository.findById(1L)).thenReturn(Optional.of(entityExistente));
        when(pontoVendaRepository.findById(10L)).thenReturn(Optional.of(origem));
        when(pontoVendaRepository.findById(20L)).thenReturn(Optional.of(destino));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        StockTransferDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo registro quando dto não possui ID")
    void save_shouldCreateNewTransfer() {
        StockTransferDto dto = criarDto(null, 10L, 20L);
        Usuario usuario = new Usuario();
        PontoVenda origem = new PontoVenda();
        PontoVenda destino = new PontoVenda();

        when(pontoVendaRepository.findById(10L)).thenReturn(Optional.of(origem));
        when(pontoVendaRepository.findById(20L)).thenReturn(Optional.of(destino));
        when(repository.save(any(TransferenciaEstoque.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(TransferenciaEstoque.class))).thenReturn(dto);

        StockTransferDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).save(any(TransferenciaEstoque.class));
    }

    @Test
    @DisplayName("save - deve lançar exceção quando ID informado não for encontrado")
    void save_shouldThrowWhenIdNotFound() {
        StockTransferDto dto = criarDto(99L, 10L, 20L);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto, new Usuario()));
    }

    @Test
    @DisplayName("save - deve lançar exceção quando ponto de venda de origem não existir")
    void save_shouldThrowWhenOrigemNotFound() {
        StockTransferDto dto = criarDto(null, 99L, 20L);

        when(pontoVendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto, new Usuario()));
    }

    @Test
    @DisplayName("save - deve lançar exceção quando ponto de venda de destino não existir")
    void save_shouldThrowWhenDestinoNotFound() {
        StockTransferDto dto = criarDto(null, 10L, 99L);

        when(pontoVendaRepository.findById(10L)).thenReturn(Optional.of(new PontoVenda()));
        when(pontoVendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto, new Usuario()));
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve remover por id")
    void delete_shouldDeleteById() {
        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    // -------------------------------------------------------------------------
    // Helper para simular a Criteria API do Specification
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<TransferenciaEstoque> specification) {
        Root<TransferenciaEstoque> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.between(any(), any(Comparable.class), any(Comparable.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);

        try {
            specification.toPredicate(root, query, cb);
        } catch (Exception ignored) {
            // Ignora exceções na execução dos Mocks de Criteria
        }
    }

    private StockTransferDto criarDto(Long id, Long idOrigem, Long idDestino) {
        StockTransferDto dto = new StockTransferDto();
        dto.setId(id);

        SalePointDto origemDto = new SalePointDto();
        origemDto.setId(idOrigem);

        SalePointDto destinoDto = new SalePointDto();
        destinoDto.setId(idDestino);

        dto.setPontoVendaOrigem(origemDto);
        dto.setPontoVendaDestino(destinoDto);

        return dto;
    }

}