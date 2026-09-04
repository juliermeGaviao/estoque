package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.PedidoCompra;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TipoOperacao;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.mapper.StockMapper;
import br.com.dinamica.estoque.repository.EstoqueRepository;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TransferenciaEstoqueRepository;
import br.com.dinamica.estoque.repository.VendaRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private EstoqueRepository repository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PontoVendaRepository pontoVendaRepository;

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private PedidoCompraRepository pedidoCompraRepository;

    @Mock
    private TransferenciaEstoqueRepository transferenciaEstoqueRepository;

    @Mock
    private StockMapper modelMapper;

    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StockServiceImpl(
                repository,
                produtoRepository,
                pontoVendaRepository,
                vendaRepository,
                pedidoCompraRepository,
                transferenciaEstoqueRepository,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // saleStock()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saleStock - deve retornar null se a quantidade for nula ou zero")
    void saleStock_shouldReturnNullWhenAmountIsNullOrEmpty() {
        assertNull(service.saleStock(1L, 1L, 1L, null, new Usuario()));
        assertNull(service.saleStock(1L, 1L, 1L, 0, new Usuario()));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("saleStock - deve registrar estoque de venda com sucesso (saldo previo nulo, tipo D)")
    void saleStock_shouldSaveSaleStock() {
        Long idProduto = 1L;
        Long idPontoVenda = 2L;
        Long idVenda = 3L;
        Integer amount = -5;
        Usuario usuario = new Usuario();

        Venda venda = new Venda();
        when(repository.getStockByProductAndSalePoint(idProduto, idPontoVenda)).thenReturn(null);
        when(produtoRepository.findById(idProduto)).thenReturn(Optional.of(new Produto()));
        when(pontoVendaRepository.findById(idPontoVenda)).thenReturn(Optional.of(new PontoVenda()));
        when(vendaRepository.findById(idVenda)).thenReturn(Optional.of(venda));
        when(repository.save(any(Estoque.class))).thenAnswer(i -> i.getArgument(0));

        Estoque result = service.saleStock(idProduto, idPontoVenda, idVenda, amount, usuario);

        assertNotNull(result);
        assertEquals(venda, result.getVenda());
        assertEquals(5, result.getQuantidade());
        assertEquals(TipoOperacao.D, result.getTipoOperacao());
        assertEquals(-5, result.getSaldo());
        verify(repository).save(any(Estoque.class));
    }

    // -------------------------------------------------------------------------
    // addStock()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("addStock - deve retornar null se a quantidade for nula ou zero")
    void addStock_shouldReturnNullWhenAmountIsNullOrEmpty() {
        assertNull(service.addStock(1L, 1L, 1L, null, new Usuario()));
        assertNull(service.addStock(1L, 1L, 1L, 0, new Usuario()));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addStock - deve registrar entrada de estoque com sucesso (saldo previo existente, tipo C)")
    void addStock_shouldSaveAddStock() {
        Long idProduto = 1L;
        Long idPontoVenda = 2L;
        Long idPedidoCompra = 3L;
        Integer amount = 10;
        Usuario usuario = new Usuario();

        PedidoCompra pedidoCompra = new PedidoCompra();
        when(repository.getStockByProductAndSalePoint(idProduto, idPontoVenda)).thenReturn(20);
        when(produtoRepository.findById(idProduto)).thenReturn(Optional.of(new Produto()));
        when(pontoVendaRepository.findById(idPontoVenda)).thenReturn(Optional.of(new PontoVenda()));
        when(pedidoCompraRepository.findById(idPedidoCompra)).thenReturn(Optional.of(pedidoCompra));
        when(repository.save(any(Estoque.class))).thenAnswer(i -> i.getArgument(0));

        Estoque result = service.addStock(idProduto, idPontoVenda, idPedidoCompra, amount, usuario);

        assertNotNull(result);
        assertEquals(pedidoCompra, result.getPedidoCompra());
        assertEquals(10, result.getQuantidade());
        assertEquals(TipoOperacao.C, result.getTipoOperacao());
        assertEquals(30, result.getSaldo());
        verify(repository).save(any(Estoque.class));
    }

    // -------------------------------------------------------------------------
    // Consultas Simples de Estoque
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getStock - deve retornar o valor ou zero quando for nulo")
    void getStock_shouldReturnStockOrZero() {
        when(repository.getStockByProduct(1L)).thenReturn(15);
        assertEquals(15, service.getStock(1L));

        when(repository.getStockByProduct(2L)).thenReturn(null);
        assertEquals(0, service.getStock(2L));
    }

    @Test
    @DisplayName("getStockSalePoint - deve retornar o valor ou zero quando for nulo")
    void getStockSalePoint_shouldReturnStockOrZero() {
        when(repository.getStockByProductAndSalePoint(1L, 1L)).thenReturn(8);
        assertEquals(8, service.getStockSalePoint(1L, 1L));

        when(repository.getStockByProductAndSalePoint(2L, 2L)).thenReturn(null);
        assertEquals(0, service.getStockSalePoint(2L, 2L));
    }

    @Test
    @DisplayName("getStockPurchaseOrder - deve retornar o valor ou zero quando for nulo")
    void getStockPurchaseOrder_shouldReturnStockOrZero() {
        when(repository.getStockByProductAndPurchaseOrder(1L, 1L)).thenReturn(50);
        assertEquals(50, service.getStockPurchaseOrder(1L, 1L));

        when(repository.getStockByProductAndPurchaseOrder(2L, 2L)).thenReturn(null);
        assertEquals(0, service.getStockPurchaseOrder(2L, 2L));
    }

    // -------------------------------------------------------------------------
    // Consultas de Listas Mapeadas
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getPurchaseOrderProducts - deve mapear lista para DTOs")
    void getPurchaseOrderProducts_shouldReturnDtoList() {
        Estoque entity = new Estoque();
        StockDto dto = new StockDto();

        when(repository.getStockByPurchaseOrder(1L)).thenReturn(List.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        List<StockDto> result = service.getPurchaseOrderProducts(1L);

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    @DisplayName("getStockBySalePoint - deve mapear lista para DTOs")
    void getStockBySalePoint_shouldReturnDtoList() {
        Estoque entity = new Estoque();
        StockDto dto = new StockDto();

        when(repository.getStockBySalePoint(1L)).thenReturn(List.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        List<StockDto> result = service.getStockBySalePoint(1L);

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    @DisplayName("getStockTransferProducts - deve mapear lista para DTOs")
    void getStockTransferProducts_shouldReturnDtoList() {
        Estoque entity = new Estoque();
        StockDto dto = new StockDto();

        when(repository.getStockByStockTransfer(1L)).thenReturn(List.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        List<StockDto> result = service.getStockTransferProducts(1L);

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    @DisplayName("getStockByProductAndSalePoint - deve mapear lista para DTOs")
    void getStockByProductAndSalePoint_shouldReturnDtoList() {
        Estoque entity = new Estoque();
        StockDto dto = new StockDto();

        when(repository.getStockByProductAndSalePoint(1L)).thenReturn(List.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        List<StockDto> result = service.getStockByProductAndSalePoint(1L);

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    // -------------------------------------------------------------------------
    // transferStock()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("transferStock - deve transferir estoque entre dois pontos de venda")
    void transferStock_shouldTransferStockSuccessfully() {
        Long idProduto = 1L;
        Long idOrigem = 10L;
        Long idDestino = 20L;
        Long idTransferencia = 100L;
        Integer amount = 5;
        Usuario usuario = new Usuario();

        TransferenciaEstoque transferencia = new TransferenciaEstoque();

        when(produtoRepository.findById(idProduto)).thenReturn(Optional.of(new Produto()));
        when(pontoVendaRepository.findById(idOrigem)).thenReturn(Optional.of(new PontoVenda()));
        when(pontoVendaRepository.findById(idDestino)).thenReturn(Optional.of(new PontoVenda()));
        when(transferenciaEstoqueRepository.findById(idTransferencia)).thenReturn(Optional.of(transferencia));

        service.transferStock(idProduto, idOrigem, idDestino, idTransferencia, amount, usuario);

        verify(repository, times(2)).save(any(Estoque.class));
    }

    @Test
    @DisplayName("transferStock - deve lançar exceção se transferencia nao for encontrada")
    void transferStock_shouldThrowWhenTransferNotFound() {
        Long idProduto = 1L;
        Long idOrigem = 10L;
        Long idDestino = 20L;
        Long idTransferencia = 99L;

        when(produtoRepository.findById(idProduto)).thenReturn(Optional.of(new Produto()));
        when(pontoVendaRepository.findById(idOrigem)).thenReturn(Optional.of(new PontoVenda()));
        when(pontoVendaRepository.findById(idDestino)).thenReturn(Optional.of(new PontoVenda()));
        when(transferenciaEstoqueRepository.findById(idTransferencia)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> 
            service.transferStock(idProduto, idOrigem, idDestino, idTransferencia, 5, new Usuario())
        );
    }

    // -------------------------------------------------------------------------
    // undoSale()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("undoSale - deve estornar venda desvinculando e registrando novo estoque")
    void undoSale_shouldRevertSaleStock() {
        Long idVenda = 1L;
        Usuario usuario = new Usuario();

        Produto produto = new Produto();
        produto.setId(10L);

        PontoVenda pontoVenda = new PontoVenda();
        pontoVenda.setId(20L);

        Estoque estoqueVenda = new Estoque();
        estoqueVenda.setProduto(produto);
        estoqueVenda.setPontoVenda(pontoVenda);
        estoqueVenda.setQuantidade(3);
        estoqueVenda.setVenda(new Venda());

        when(repository.findByVenda(idVenda)).thenReturn(List.of(estoqueVenda));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(pontoVendaRepository.findById(20L)).thenReturn(Optional.of(pontoVenda));

        service.undoSale(idVenda, usuario);

        // Salva 2 vezes por item da venda: 1 do getNewStock (estorno) e 1 da atualização desvinculando a venda
        verify(repository, times(2)).save(any(Estoque.class));
        assertNull(estoqueVenda.getVenda());
    }
}