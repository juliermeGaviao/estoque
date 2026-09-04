package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductFilterDto;
import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProductMapper;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.StockService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProdutoRepository repository;

    @Mock
    private TipoProdutoRepository tipoProdutoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private StockService stockService;

    @Mock
    private ProductMapper modelMapper;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(
                repository,
                tipoProdutoRepository,
                fornecedorRepository,
                stockService,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar produto por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        Produto entity = new Produto();
        entity.setId(id);
        ProductDto dto = new ProductDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        ProductDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(ProductFilterDto filter, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros (inclusive between para minPeso e maxPeso) e enriquecer com estoque")
    void list_shouldApplyAllFiltersIncludingBetweenWeight() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductFilterDto filter = new ProductFilterDto();
        filter.setNome("Parafuso");
        filter.setReferencia("REF-123");
        filter.setIdTipoProduto(2L);
        filter.setIdFornecedor(3L);
        filter.setMinPeso(1);
        filter.setMaxPeso(5);

        Produto entity = new Produto();
        entity.setId(10L);
        ProductDto dto = new ProductDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Produto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);
        when(stockService.getStock(10L)).thenReturn(50);

        Page<ProductDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(50, result.getContent().get(0).getEstoque());
        verify(stockService).getStock(10L);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro de minPeso apenas (greaterThanOrEqualTo)")
    void list_shouldApplyMinWeightFilterOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductFilterDto filter = new ProductFilterDto();
        filter.setMinPeso(1);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Produto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProductDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro de maxPeso apenas (lessThanOrEqualTo)")
    void list_shouldApplyMaxWeightFilterOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductFilterDto filter = new ProductFilterDto();
        filter.setMaxPeso(5);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Produto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProductDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve ignorar campos texto quando forem vazios ou em branco")
    void list_shouldIgnoreBlankStringFilters(String blank) {
        Pageable pageable = PageRequest.of(0, 10);
        ProductFilterDto filter = new ProductFilterDto();
        filter.setNome(blank);
        filter.setReferencia(blank);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Produto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProductDto> result = service.list(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(ProductDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar produto existente e definir saldo de estoque retornado pelo stockService")
    void save_shouldUpdateExistingProduct() {
        Long id = 1L;
        Long tipoProdutoId = 10L;
        Long fornecedorId = 20L;

        ProductDto dto = createSampleDto(id, tipoProdutoId, fornecedorId);
        Usuario usuario = new Usuario();

        Produto entityExistente = new Produto();
        entityExistente.setId(id);
        TipoProduto tipoProduto = new TipoProduto();
        Fornecedor fornecedor = new Fornecedor();

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(tipoProdutoRepository.findById(tipoProdutoId)).thenReturn(Optional.of(tipoProduto));
        when(fornecedorRepository.findById(fornecedorId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(stockService.getStock(id)).thenReturn(15);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        ProductDto result = service.save(dto, usuario);

        assertNotNull(result);
        assertEquals(15, result.getEstoque());
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar produto novo quando ID for nulo e atribuir estoque 0 caso stockService retorne nulo")
    void save_shouldCreateNewProductAndFallbackStockToZeroWhenNull() {
        Long tipoProdutoId = 10L;
        Long fornecedorId = 20L;

        ProductDto dto = createSampleDto(null, tipoProdutoId, fornecedorId);
        Usuario usuario = new Usuario();

        Produto entitySalva = new Produto();
        entitySalva.setId(100L);
        TipoProduto tipoProduto = new TipoProduto();
        Fornecedor fornecedor = new Fornecedor();

        when(tipoProdutoRepository.findById(tipoProdutoId)).thenReturn(Optional.of(tipoProduto));
        when(fornecedorRepository.findById(fornecedorId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(any(Produto.class))).thenReturn(entitySalva);
        when(stockService.getStock(100L)).thenReturn(null);
        when(modelMapper.toDto(entitySalva)).thenReturn(dto);

        ProductDto result = service.save(dto, usuario);

        assertNotNull(result);
        assertEquals(0, result.getEstoque());
        verify(repository, never()).findById(any());
        verify(repository).save(any(Produto.class));
    }

    // -------------------------------------------------------------------------
    // save(List<ProductDto> dtos, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save list - deve iterar e salvar todos os DTOs da lista")
    void saveList_shouldSaveAllDtos() {
        ProductDto dto1 = createSampleDto(null, 10L, 20L);
        ProductDto dto2 = createSampleDto(null, 10L, 20L);
        List<ProductDto> dtos = List.of(dto1, dto2);
        Usuario usuario = new Usuario();

        Produto entity = new Produto();
        entity.setId(1L);

        when(tipoProdutoRepository.findById(10L)).thenReturn(Optional.of(new TipoProduto()));
        when(fornecedorRepository.findById(20L)).thenReturn(Optional.of(new Fornecedor()));
        when(repository.save(any(Produto.class))).thenReturn(entity);
        when(modelMapper.toDto(any(Produto.class))).thenReturn(new ProductDto());

        service.save(dtos, usuario);

        verify(repository, times(2)).save(any(Produto.class));
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
    // Helpers
    // -------------------------------------------------------------------------

    private ProductDto createSampleDto(Long id, Long tipoProdutoId, Long fornecedorId) {
        ProductDto dto = new ProductDto();
        dto.setId(id);

        ProductTypeDto tipoDto = new ProductTypeDto();
        tipoDto.setId(tipoProdutoId);
        dto.setTipoProduto(tipoDto);

        ProviderDto fornecedorDto = new ProviderDto();
        fornecedorDto.setId(fornecedorId);
        dto.setFornecedor(fornecedorDto);

        return dto;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<Produto> specification) {
        Root<Produto> root = mock(Root.class);
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