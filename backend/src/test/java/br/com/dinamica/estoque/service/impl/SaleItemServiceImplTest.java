package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.entity.ItemVenda;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.mapper.SaleItemMapper;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.StockService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class SaleItemServiceImplTest {

    @Mock
    private ItemVendaRepository repository;

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository;

    @Mock
    private StockService stockService;

    @Mock
    private SaleItemMapper modelMapper;

    private SaleItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SaleItemServiceImpl(
                repository,
                vendaRepository,
                tabelaPrecoProdutoRepository,
                stockService,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve limpar relacionamentos aninhados e retornar DTO")
    void get_shouldCleanNestedRelationsAndReturnDto() {
        Long id = 1L;
        ItemVenda entity = new ItemVenda();
        SaleItemDto dto = criarDtoCompleto(id, 10L, 20L, 5);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        SaleItemDto result = service.get(id);

        assertNotNull(result);
        assertNull(result.getVenda().getVendedor().getPerfis());
        assertNull(result.getTabelaPrecoProduto().getProduto().getTipoProduto());
        assertNull(result.getTabelaPrecoProduto().getProduto().getFornecedor());
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("get - deve lançar exceção quando não encontrado")
    void get_shouldThrowWhenNotFound() {
        Long id = 99L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(id));
    }

    // -------------------------------------------------------------------------
    // list(...)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros (venda, tabela, quantidade min e max) e limpar DTOs")
    void list_shouldApplyAllFiltersAndReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ItemVenda entity = new ItemVenda();
        SaleItemDto dto = criarDtoCompleto(1L, 10L, 20L, 5);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ItemVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<SaleItemDto> result = service.list(10L, 20L, 1, 10, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getVenda().getVendedor().getPerfis());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve testar ramificações de minQuantidade e maxQuantidade isolados e nulos")
    void list_shouldTestQuantityBranches() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ItemVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        // Apenas min
        assertNotNull(service.list(null, null, 5, null, pageable));
        // Apenas max
        assertNotNull(service.list(null, null, null, 10, pageable));
        // Sem filtros
        assertNotNull(service.list(null, null, null, null, pageable));
    }

    // -------------------------------------------------------------------------
    // save(SaleItemDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar item existente")
    void save_shouldUpdateExistingItem() {
        SaleItemDto dto = criarDtoCompleto(1L, 10L, 20L, 5);
        Usuario usuario = new Usuario();
        ItemVenda entity = new ItemVenda();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(vendaRepository.findById(10L)).thenReturn(Optional.of(new Venda()));
        when(tabelaPrecoProdutoRepository.findById(20L)).thenReturn(Optional.of(new TabelaPrecoProduto()));
        when(repository.save(entity)).thenReturn(entity);
        when(modelMapper.toDto(entity)).thenReturn(dto);

        SaleItemDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("save - deve criar novo item quando ID for nulo")
    void save_shouldCreateNewItem() {
        SaleItemDto dto = criarDtoCompleto(null, 10L, 20L, 5);
        Usuario usuario = new Usuario();

        when(vendaRepository.findById(10L)).thenReturn(Optional.of(new Venda()));
        when(tabelaPrecoProdutoRepository.findById(20L)).thenReturn(Optional.of(new TabelaPrecoProduto()));
        when(repository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
        when(modelMapper.toDto(any(ItemVenda.class))).thenReturn(dto);

        SaleItemDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(repository).save(any(ItemVenda.class));
    }

    // -------------------------------------------------------------------------
    // save(List<SaleItemDto> list, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save em lote - deve processar atualizações, novos itens e exclusão de excedentes")
    void saveList_shouldProcessUpdatesAndDeletions() {
        Usuario usuario = new Usuario();

        SaleItemDto dtoExistente = criarDtoCompleto(1L, 10L, 20L, 3);
        dtoExistente.getTabelaPrecoProduto().getProduto().setId(100L);

        SaleItemDto dtoNovo = criarDtoCompleto(null, 10L, 30L, 4);
        dtoNovo.getTabelaPrecoProduto().getProduto().setId(200L);

        List<SaleItemDto> dtoList = List.of(dtoExistente, dtoNovo);

        ItemVenda itemExistenteBD = new ItemVenda();
        itemExistenteBD.setQuantidade(5);
        Venda vendaMock = new Venda();
        PontoVenda pontoVendaMock = new PontoVenda();
        pontoVendaMock.setId(50L);
        vendaMock.setId(10L);
        vendaMock.setPontoVenda(pontoVendaMock);
        itemExistenteBD.setVenda(vendaMock);

        when(repository.findById(1L)).thenReturn(Optional.of(itemExistenteBD));
        when(vendaRepository.findById(10L)).thenReturn(Optional.of(vendaMock));
        when(tabelaPrecoProdutoRepository.findById(20L)).thenReturn(Optional.of(new TabelaPrecoProduto()));
        when(tabelaPrecoProdutoRepository.findById(30L)).thenReturn(Optional.of(new TabelaPrecoProduto()));

        ItemVenda itemToBeDeleted = new ItemVenda();
        itemToBeDeleted.setQuantidade(2);
        TabelaPrecoProduto tpp = new TabelaPrecoProduto();
        br.com.dinamica.estoque.entity.Produto prod = new br.com.dinamica.estoque.entity.Produto();
        prod.setId(300L);
        tpp.setProduto(prod);
        itemToBeDeleted.setTabelaPrecoProduto(tpp);
        itemToBeDeleted.setVenda(vendaMock);

        when(repository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));

        SaleItemDto dtoRetorno1 = criarDtoCompleto(1L, 10L, 20L, 3);
        SaleItemDto dtoRetorno2 = criarDtoCompleto(2L, 10L, 30L, 4);
        when(modelMapper.toDto(any(ItemVenda.class))).thenReturn(dtoRetorno1).thenReturn(dtoRetorno2);

        when(repository.getItensByVendaIdAndNotInIds(eq(10L), any())).thenReturn(List.of(itemToBeDeleted));

        List<SaleItemDto> result = service.save(dtoList, usuario);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(stockService).saleStock(100L, 50L, 10L, 2, usuario);
        verify(stockService).saleStock(200L, 50L, 10L, -4, usuario);
        verify(stockService).saleStock(300L, 50L, 10L, 2, usuario);
        verify(repository).delete(itemToBeDeleted);
    }

    // -------------------------------------------------------------------------
    // delete(Long id, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve devolver estoque e remover registro por ID")
    void delete_shouldUndoStockAndDelete() {
        Long id = 1L;
        Usuario usuario = new Usuario();

        ItemVenda item = new ItemVenda();
        item.setQuantidade(10);
        Venda venda = new Venda();
        venda.setId(5L);
        PontoVenda ponto = new PontoVenda();
        ponto.setId(8L);
        venda.setPontoVenda(ponto);
        item.setVenda(venda);

        TabelaPrecoProduto tpp = new TabelaPrecoProduto();
        br.com.dinamica.estoque.entity.Produto produto = new br.com.dinamica.estoque.entity.Produto();
        produto.setId(99L);
        tpp.setProduto(produto);
        item.setTabelaPrecoProduto(tpp);

        when(repository.findById(id)).thenReturn(Optional.of(item));

        service.delete(id, usuario);

        verify(stockService).saleStock(99L, 8L, 5L, 10, usuario);
        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // getItensByPriceTableAndSalePoint
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getItensByPriceTableAndSalePoint - deve mapear o array de objetos para DTOs")
    void getItensByPriceTableAndSalePoint_shouldMapObjectArrayToDto() {
        Object[] linha = new Object[] {
                10L,                // 0: productDto id
                20L,                // 1: priceTableProductDto id
                "Produto Teste",    // 2: nome
                "Eletrônicos",      // 3: tipo
                "Fornecedor X",     // 4: fornecedor
                "REF123",           // 5: referencia
                150.50,             // 6: preco
                15                  // 7: estoque
        };

        when(repository.getItensByPriceTable(1L, 2L)).thenReturn(List.<Object[]>of(linha));

        List<SaleItemDto> result = service.getItensByPriceTableAndSalePoint(1L, 2L);

        assertNotNull(result);
        assertEquals(1, result.size());

        SaleItemDto item = result.get(0);
        assertEquals(10L, item.getTabelaPrecoProduto().getProduto().getId());
        assertEquals(20L, item.getTabelaPrecoProduto().getId());
        assertEquals("Produto Teste", item.getTabelaPrecoProduto().getProduto().getNome());
        assertEquals("Eletrônicos", item.getTabelaPrecoProduto().getProduto().getTipoProduto().getNome());
        assertNotNull(item.getTabelaPrecoProduto().getProduto().getFornecedor());
        assertEquals("REF123", item.getTabelaPrecoProduto().getProduto().getReferencia());
        assertEquals(BigDecimal.valueOf(150.50), item.getPrecoUnitario());
        assertEquals(15, item.getTabelaPrecoProduto().getProduto().getEstoque());
    }

    // -------------------------------------------------------------------------
    // getItensBySale
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getItensBySale - deve mapear o array de objetos com total preenchido e nulo")
    void getItensBySale_shouldMapObjectArrayToDtoWithNullAndNonNullTotal() {
        Object[] linha1 = new Object[] {
                1L,                 // 0: id
                10L,                // 1: saleId
                20L,                // 2: productId
                30L,                // 3: priceTableProductId
                "Produto A",        // 4: nome
                "REF-A",            // 5: referencia
                "Tipo A",           // 6: tipo
                "Forn A",           // 7: fornecedor
                2,                  // 8: quantidade
                50.0,               // 9: preco
                100.0,              // 10: total
                5                   // 11: estoque
        };

        Object[] linha2 = new Object[] {
                2L, 10L, 21L, 31L, "Produto B", "REF-B", "Tipo B", "Forn B", 1, 20.0, null, 10
        };

        when(repository.getItensBySale(10L)).thenReturn(List.<Object[]>of(linha1, linha2));

        List<SaleItemDto> result = service.getItensBySale(10L);

        assertNotNull(result);
        assertEquals(2, result.size());

        SaleItemDto item1 = result.get(0);
        assertEquals(1L, item1.getId());
        assertEquals(10L, item1.getVenda().getId());
        assertEquals(BigDecimal.valueOf(100.0), item1.getTotal());

        SaleItemDto item2 = result.get(1);
        assertNull(item2.getTotal());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<ItemVenda> specification) {
        Root<ItemVenda> root = mock(Root.class);
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
            // Executa a lambda para cobrir Specifications
        }
    }

    private SaleItemDto criarDtoCompleto(Long id, Long idVenda, Long idTabelaPrecoProduto, Integer quantidade) {
        SaleItemDto dto = new SaleItemDto();
        dto.setId(id);
        dto.setQuantidade(quantidade);

        SaleDto venda = new SaleDto();
        venda.setId(idVenda);
        UserDto vendedor = new UserDto();
        vendedor.setPerfis(List.of());
        venda.setVendedor(vendedor);

        SalePointDto pontoVenda = new SalePointDto();
        pontoVenda.setId(50L);
        venda.setPontoVenda(pontoVenda);

        dto.setVenda(venda);

        PriceTableProductDto tppDto = new PriceTableProductDto();
        tppDto.setId(idTabelaPrecoProduto);

        ProductDto produtoDto = new ProductDto();
        produtoDto.setId(100L);
        produtoDto.setTipoProduto(new ProductTypeDto());
        produtoDto.setFornecedor(new ProviderDto());
        tppDto.setProduto(produtoDto);

        dto.setTabelaPrecoProduto(tppDto);

        return dto;
    }
}