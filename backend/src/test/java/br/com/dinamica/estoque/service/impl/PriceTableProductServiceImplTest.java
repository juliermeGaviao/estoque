package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.PriceTableProductFilterDto;
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PriceTableProductMapper;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class PriceTableProductServiceImplTest {

    @Mock
    private TabelaPrecoProdutoRepository repository;

    @Mock
    private TabelaPrecoRepository tabelaPrecoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PriceTableProductMapper modelMapper;

    @Mock
    private Query query;

    @Mock
    private Query countQuery;

    @InjectMocks
    private PriceTableProductServiceImpl service;

    private Usuario usuario;
    private TabelaPrecoProduto entity;
    private PriceTableProductDto dto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();

        entity = new TabelaPrecoProduto();
        entity.setId(10L);

        dto = new PriceTableProductDto();
        dto.setId(10L);
        dto.setPreco(new BigDecimal("150.00"));

        PriceTableDto tabelaDto = new PriceTableDto();
        tabelaDto.setId(1L);
        dto.setTabela(tabelaDto);

        ProductDto produtoDto = new ProductDto();
        produtoDto.setId(2L);
        dto.setProduto(produtoDto);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar a entidade")
    void get_success() {
        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PriceTableProductDto result = service.get(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    // -------------------------------------------------------------------------
    // list(Long idTabelaPreco, Long idProduto, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    @DisplayName("list - deve buscar com filtros e retornar paginado executando as lambdas de Specification")
    void list_withFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TabelaPrecoProduto> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenAnswer(invocation -> {
            Specification<TabelaPrecoProduto> spec = invocation.getArgument(0);

            // Criar mocks para simular a execução do Criteria Builder sem disparar NullPointerException
            jakarta.persistence.criteria.Root<TabelaPrecoProduto> root = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class);
            jakarta.persistence.criteria.CriteriaQuery<?> query = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class);
            jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class);
            jakarta.persistence.criteria.Path path = org.mockito.Mockito.mock(jakarta.persistence.criteria.Path.class);

            when(root.get(anyString())).thenReturn(path);
            when(path.get(anyString())).thenReturn(path);

            // Força a execução das lambdas internas para garantir cobertura sem estourar NullPointerException
            spec.toPredicate(root, query, cb);

            return entityPage;
        });

        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PriceTableProductDto> result = service.list(1L, 2L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve buscar sem filtros (ids nulos)")
    void list_nullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TabelaPrecoProduto> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PriceTableProductDto> result = service.list(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // save(PriceTableProductDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar preco quando ID ja existe")
    void save_updateExisting() {
        TabelaPreco tabela = new TabelaPreco();
        Produto produto = new Produto();

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(tabelaPrecoRepository.findById(1L)).thenReturn(Optional.of(tabela));
        when(produtoRepository.findById(2L)).thenReturn(Optional.of(produto));
        when(repository.save(any(TabelaPrecoProduto.class))).thenReturn(entity);
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PriceTableProductDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("save - deve criar novo preco quando ID for nulo")
    void save_createNew() {
        dto.setId(null);
        TabelaPreco tabela = new TabelaPreco();
        Produto produto = new Produto();

        when(tabelaPrecoRepository.findById(1L)).thenReturn(Optional.of(tabela));
        when(produtoRepository.findById(2L)).thenReturn(Optional.of(produto));
        when(repository.save(any(TabelaPrecoProduto.class))).thenReturn(entity);
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PriceTableProductDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).save(any(TabelaPrecoProduto.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve deletar por id")
    void delete() {
        service.delete(10L);
        verify(repository).deleteById(10L);
    }

    // -------------------------------------------------------------------------
    // savePrices(List<PriceTableProductDto> prices, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("savePrices - deve salvar quando tiver preco, deletar quando preco for nulo com ID e ignorar quando ambos nulos")
    void savePrices_allBranches() {
        PriceTableProductDto itemComPreco = new PriceTableProductDto();
        itemComPreco.setId(10L);
        itemComPreco.setPreco(new BigDecimal("100.00"));
        itemComPreco.setTabela(dto.getTabela());
        itemComPreco.setProduto(dto.getProduto());

        PriceTableProductDto itemParaDeletar = new PriceTableProductDto();
        itemParaDeletar.setId(20L);
        itemParaDeletar.setPreco(null);

        PriceTableProductDto itemIgnorado = new PriceTableProductDto();
        itemIgnorado.setId(null);
        itemIgnorado.setPreco(null);

        TabelaPreco tabela = new TabelaPreco();
        Produto produto = new Produto();

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(tabelaPrecoRepository.findById(1L)).thenReturn(Optional.of(tabela));
        when(produtoRepository.findById(2L)).thenReturn(Optional.of(produto));

        service.savePrices(List.of(itemComPreco, itemParaDeletar, itemIgnorado), usuario);

        verify(repository).save(any(TabelaPrecoProduto.class));
        verify(repository).deleteById(20L);
        verify(repository, never()).deleteById(10L);
    }

    // -------------------------------------------------------------------------
    // getProductsByTable(PriceTableProductFilterDto filter, Pageable pageable)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProductsByTable - deve cobrir todas as clausulas dinâmicas da query, sort e mapeamento")
    void getProductsByTable_fullFilterAndSort() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setNome("Notebook");
        filter.setReferencia("REF123");
        filter.setIdTipoProduto(5L);
        filter.setIdFornecedor(8L);
        filter.setMinPeso(100);
        filter.setMaxPeso(500);
        filter.setMinPreco(10);
        filter.setMaxPreco(100);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "produto.nome"));

        Object[] linha1 = new Object[] { 1L, "Eletrônicos", "Tech Corp", "REF123", "Notebook", 200, 10L, 99.90 };
        Object[] linha2 = new Object[] { 2L, "Eletrônicos", "Tech Corp", "REF124", "Mouse", 50, 11L, null };

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(linha1, linha2));

        Query mockCountQuery = countQuery;
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(mockCountQuery);
        when(mockCountQuery.getSingleResult()).thenReturn(2L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        assertEquals(new BigDecimal("99.9"), page.getContent().get(0).getPreco());
        assertNull(page.getContent().get(1).getPreco());

        verify(query).setParameter("idTabelaPreco", 1L);
        verify(query).setParameter("nome", "%notebook%");
        verify(query).setParameter("referencia", "%ref123%");
        verify(query).setParameter("idTipoProduto", 5L);
        verify(query).setParameter("idFornecedor", 8L);
        verify(query).setParameter("minPeso", 100);
        verify(query).setParameter("maxPeso", 500);
    }

    @Test
    @DisplayName("getProductsByTable - deve testar minPeso isolado e minPreco isolado sem ordenacao")
    void getProductsByTable_minFiltersOnly() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setMinPeso(100);
        filter.setMinPreco(10); // Passando Integer diretamente

        Pageable pageable = PageRequest.of(0, 10);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        verify(query).setParameter("minPeso", 100);
        verify(query).setParameter("minPreco", 10);
    }

    @Test
    @DisplayName("getProductsByTable - deve testar maxPeso isolado e maxPreco isolado")
    void getProductsByTable_maxFiltersOnly() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setMaxPeso(500);
        filter.setMaxPreco(100); // Passando Integer diretamente

        Pageable pageable = PageRequest.of(0, 5);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        verify(query).setParameter("maxPeso", 500);
        verify(query).setParameter("maxPreco", 100);
    }

    // -------------------------------------------------------------------------
    // list - Execução completa dos Predicates dentro de Specification (Linhas 74, 77, 81)
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    @DisplayName("list - deve executar internamente os Lambdas de Predicate do Specification")
    void list_executeSpecificationLambdas() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenAnswer(invocation -> {
            Specification<TabelaPrecoProduto> spec = invocation.getArgument(0);

            // Mocks para simular a chamada interna das lambdas cb.equal(...)
            jakarta.persistence.criteria.Root<TabelaPrecoProduto> root = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class);
            jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class);
            jakarta.persistence.criteria.Path path = org.mockito.Mockito.mock(jakarta.persistence.criteria.Path.class);

            when(root.get(anyString())).thenReturn(path);
            when(path.get(anyString())).thenReturn(path);

            // Força a execução das expressões lambdas encadeadas doSpecification.and(...)
            spec.toPredicate(root, null, cb);

            return new PageImpl<>(List.of(entity), pageable, 1);
        });

        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PriceTableProductDto> result = service.list(1L, 2L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // getProductsByTable - Testes para zerar as branches amarelas (Linhas 220-226 e 228-234)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getQuery - deve cobrir a branch else-if apenas maxPeso e apenas maxPreco")
    void getProductsByTable_onlyMaxPesoAndMaxPreco() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setMaxPeso(500); // Entra no 'else if (filter.getMaxPeso() != null)'
        filter.setMaxPreco(100); // Entra no 'else if (filter.getMaxPreco() != null)'

        Pageable pageable = PageRequest.of(0, 5);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        verify(query).setParameter("maxPeso", 500);
        verify(query).setParameter("maxPreco", 100);
    }

    @Test
    @DisplayName("getQuery - deve cobrir a branch else-if apenas minPeso e apenas minPreco")
    void getProductsByTable_onlyMinPesoAndMinPreco() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setMinPeso(100); // Entra no 'else if (filter.getMinPeso() != null)'
        filter.setMinPreco(10); // Entra no 'else if (filter.getMinPreco() != null)'

        Pageable pageable = PageRequest.of(0, 5);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        verify(query).setParameter("minPeso", 100);
        verify(query).setParameter("minPreco", 10);
    }

    @Test
    @DisplayName("getQuery - deve cobrir o primeiro 'if' com ambos min e max informados")
    void getProductsByTable_bothMinAndMax() {
        PriceTableProductFilterDto filter = new PriceTableProductFilterDto();
        filter.setIdTabelaPreco(1L);
        filter.setMinPeso(100);
        filter.setMaxPeso(500); // Entra no 'if (filter.getMinPeso() != null && filter.getMaxPeso() != null)'
        filter.setMinPreco(10);
        filter.setMaxPreco(100); // Entra no 'if (filter.getMinPreco() != null && filter.getMaxPreco() != null)'

        Pageable pageable = PageRequest.of(0, 5);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery("SELECT count(*) FROM produto p")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<PriceTableProductDto> page = service.getProductsByTable(filter, pageable);

        assertNotNull(page);
        verify(query).setParameter("minPeso", 100);
        verify(query).setParameter("maxPeso", 500);
        verify(query).setParameter("minPreco", 10);
        verify(query).setParameter("maxPreco", 100);
    }

}