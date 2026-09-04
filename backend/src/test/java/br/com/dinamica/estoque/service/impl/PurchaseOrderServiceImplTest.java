package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.PedidoCompra;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PurchaseOrderMapper;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    @Mock
    private PedidoCompraRepository repository;

    @Mock
    private FornecedorRepository providerRepository;

    @Mock
    private PurchaseOrderMapper modelMapper;

    private PurchaseOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderServiceImpl(repository, providerRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o pedido de compra por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        PedidoCompra entity = new PedidoCompra();
        entity.setId(id);
        PurchaseOrderDto dto = new PurchaseOrderDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PurchaseOrderDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(PurchaseOrderFilterDto filter, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro de numeroPedido, idFornecedor e minDataPedido + maxDataPedido (between)")
    void list_shouldApplyBetweenDateFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrderFilterDto filter = new PurchaseOrderFilterDto();
        filter.setNumeroPedido("PED-123");
        filter.setIdFornecedor(10L);
        filter.setMinDataPedido(LocalDate.of(2026, 1, 1));
        filter.setMaxDataPedido(LocalDate.of(2026, 12, 31));

        PedidoCompra entity = new PedidoCompra();
        PurchaseOrderDto dto = new PurchaseOrderDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PedidoCompra> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PurchaseOrderDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar apenas minDataPedido (greaterThanOrEqualTo)")
    void list_shouldApplyMinDateFilterOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrderFilterDto filter = new PurchaseOrderFilterDto();
        filter.setMinDataPedido(LocalDate.of(2026, 1, 1));

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PedidoCompra> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PurchaseOrderDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar apenas maxDataPedido (lessThanOrEqualTo)")
    void list_shouldApplyMaxDateFilterOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrderFilterDto filter = new PurchaseOrderFilterDto();
        filter.setMaxDataPedido(LocalDate.of(2026, 12, 31));

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PedidoCompra> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PurchaseOrderDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve ignorar numeroPedido quando vazio ou em branco")
    void list_shouldIgnoreBlankNumeroPedido(String blank) {
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrderFilterDto filter = new PurchaseOrderFilterDto();
        filter.setNumeroPedido(blank);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PedidoCompra> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PurchaseOrderDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(PurchaseOrderDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar pedido de compra existente")
    void save_shouldUpdateExistingOrder() {
        Long id = 1L;
        Long providerId = 5L;

        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(id);
        ProviderDto providerDto = new ProviderDto();
        providerDto.setId(providerId);
        dto.setFornecedor(providerDto);

        Usuario usuario = new Usuario();
        PedidoCompra entityExistente = new PedidoCompra();
        entityExistente.setId(id);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(providerId);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        PurchaseOrderDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(providerRepository).findById(providerId);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo pedido de compra quando ID for nulo")
    void save_shouldCreateNewOrder() {
        Long providerId = 5L;

        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(null);
        ProviderDto providerDto = new ProviderDto();
        providerDto.setId(providerId);
        dto.setFornecedor(providerDto);

        Usuario usuario = new Usuario();
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(providerId);

        when(providerRepository.findById(providerId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(any(PedidoCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(PedidoCompra.class))).thenReturn(dto);

        PurchaseOrderDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(PedidoCompra.class));
        verify(providerRepository).findById(providerId);
        verify(repository).save(any(PedidoCompra.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve invocar deleteById no repositorio")
    void delete_shouldCallRepositoryDeleteById() {
        Long id = 1L;

        service.delete(id);

        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // findByOrderNumber(String numeroPedido)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByOrderNumber - deve retornar lista mapeada para DTOs")
    void findByOrderNumber_shouldReturnMappedDtoList() {
        String numeroPedido = "PED-001";
        PedidoCompra entity = new PedidoCompra();
        PurchaseOrderDto dto = new PurchaseOrderDto();

        when(repository.findByOrderNumber(numeroPedido)).thenReturn(List.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        List<PurchaseOrderDto> result = service.findByOrderNumber(numeroPedido);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
        verify(repository).findByOrderNumber(numeroPedido);
    }

    // -------------------------------------------------------------------------
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<PedidoCompra> specification) {
        Root<PedidoCompra> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Expression stringExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(stringExpression);
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.between(any(), any(Comparable.class), any(Comparable.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);

        specification.toPredicate(root, query, cb);
    }
}