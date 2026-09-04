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

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProductTypeMapper;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProductTypeServiceImplTest {

    @Mock
    private TipoProdutoRepository repository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ProductTypeMapper modelMapper;

    private ProductTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductTypeServiceImpl(repository, produtoRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o tipo de produto por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        TipoProduto entity = new TipoProduto();
        entity.setId(id);
        ProductTypeDto dto = new ProductTypeDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        ProductTypeDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(String nome, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina filtrada por nome e executar a lambda da Specification")
    void list_shouldReturnPageWithNameFilter() {
        String nome = "Eletronicos";
        Pageable pageable = PageRequest.of(0, 10);
        TipoProduto entity = new TipoProduto();
        ProductTypeDto dto = new ProductTypeDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TipoProduto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<ProductTypeDto> result = service.list(nome, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve retornar pagina sem filtro quando nome for vazio ou em branco")
    void list_shouldReturnPageWithoutFilterWhenNameIsBlank(String nome) {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TipoProduto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProductTypeDto> result = service.list(nome, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem filtro quando nome for nulo")
    void list_shouldReturnPageWithoutFilterWhenNameIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TipoProduto> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProductTypeDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(ProductTypeDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar tipo de produto existente")
    void save_shouldUpdateExistingProductType() {
        Long id = 1L;
        ProductTypeDto dto = new ProductTypeDto();
        dto.setId(id);

        Usuario usuario = new Usuario();
        TipoProduto entityExistente = new TipoProduto();
        entityExistente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        ProductTypeDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo tipo de produto quando ID for nulo")
    void save_shouldCreateNewProductType() {
        ProductTypeDto dto = new ProductTypeDto();
        dto.setId(null);

        Usuario usuario = new Usuario();

        when(repository.save(any(TipoProduto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(TipoProduto.class))).thenReturn(dto);

        ProductTypeDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(TipoProduto.class));
        verify(repository).save(any(TipoProduto.class));
    }

    // -------------------------------------------------------------------------
    // save(List<ProductTypeDto> dtos, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save list - deve iterar e salvar todos os DTOs da lista")
    void saveList_shouldSaveAllDtos() {
        ProductTypeDto dto1 = new ProductTypeDto();
        ProductTypeDto dto2 = new ProductTypeDto();
        List<ProductTypeDto> dtos = List.of(dto1, dto2);
        Usuario usuario = new Usuario();

        when(repository.save(any(TipoProduto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(dtos, usuario);

        verify(repository, times(2)).save(any(TipoProduto.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve deletar produtos associados e em seguida deletar o tipo de produto")
    void delete_shouldDeleteProductsAndProductType() {
        Long id = 1L;

        service.delete(id);

        verify(produtoRepository).deleteByTipoProduto_Id(id);
        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<TipoProduto> specification) {
        Root<TipoProduto> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Expression stringExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(stringExpression);
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);

        specification.toPredicate(root, query, cb);
    }
}