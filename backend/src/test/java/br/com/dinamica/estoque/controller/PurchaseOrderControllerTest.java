package br.com.dinamica.estoque.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.dto.StockProductDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.service.PurchaseOrderService;
import br.com.dinamica.estoque.service.StockService;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PurchaseOrderService service;

    @Mock
    private ProductService productService;

    @Mock
    private StockService stockService;

    @InjectMocks
    private PurchaseOrderController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /purchase-order - Deve buscar pedido de compra por ID")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new PurchaseOrderDto());

        mockMvc.perform(get("/purchase-order").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/purchase-order").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pedido de compra não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /purchase-order/list - Deve listar pedidos ordenando por desc quando sort.length > 1")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(PurchaseOrderFilterDto.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/purchase-order/list")
                        .param("numeroPedido", "PO-123")
                        .param("idFornecedor", "1")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/list - Deve listar pedidos ordenando por asc quando sort.length > 1")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(PurchaseOrderFilterDto.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/purchase-order/list")
                        .param("sort", "id", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/list - Deve usar direcao padrao 'asc' quando sort.length == 1")
    void list_ShouldReturnOk_SingleSortParam() throws Exception {
        when(service.list(any(PurchaseOrderFilterDto.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/purchase-order/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(PurchaseOrderFilterDto.class), any(Pageable.class)))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/purchase-order/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar pedidos de venda."));
    }

    @Test
    @DisplayName("POST /purchase-order - Deve salvar pedido e incrementar estoque")
    void save_ShouldReturnOkAndAddStock() throws Exception {
        StockProductDto itemEstoque = new StockProductDto();
        itemEstoque.setIdProduto(10L);
        itemEstoque.setQuantidade(5);

        PurchaseOrderDto dtoInput = new PurchaseOrderDto();
        dtoInput.setEstoque(List.of(itemEstoque));

        PurchaseOrderDto dtoSaved = new PurchaseOrderDto();
        dtoSaved.setId(100L);

        when(service.save(any(PurchaseOrderDto.class), any(Usuario.class))).thenReturn(dtoSaved);

        mockMvc.perform(post("/purchase-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isOk());

        verify(stockService).addStock(eq(10L), eq(1L), eq(100L), eq(5), any(Usuario.class));
    }

    @Test
    @DisplayName("POST /purchase-order - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        PurchaseOrderDto dtoInput = new PurchaseOrderDto();
        dtoInput.setId(99L);
        dtoInput.setEstoque(List.of());

        when(service.save(any(PurchaseOrderDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/purchase-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pedido de compra não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /purchase-order - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        PurchaseOrderDto dtoInput = new PurchaseOrderDto();

        when(service.save(any(PurchaseOrderDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/purchase-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar pedido de compra."));
    }

    @Test
    @DisplayName("DELETE /purchase-order - Deve deletar pedido com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/purchase-order").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /purchase-order - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/purchase-order").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pedido de compra não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /purchase-order - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/purchase-order").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover pedido de compra."));
    }

    @Test
    @DisplayName("GET /purchase-order/list-products - Deve listar produtos por fornecedor")
    void listProducts_ShouldReturnOk() throws Exception {
        Long idFornecedor = 1L;
        ProviderDto fornecedor = new ProviderDto();
        fornecedor.setId(idFornecedor);

        ProductDto produto1 = new ProductDto();
        produto1.setId(10L);
        produto1.setFornecedor(fornecedor);

        ProductDto produto2 = new ProductDto();
        produto2.setId(20L);
        ProviderDto outroFornecedor = new ProviderDto();
        outroFornecedor.setId(2L);
        produto2.setFornecedor(outroFornecedor);

        when(productService.list(any(), any())).thenReturn(new PageImpl<>(List.of(produto1, produto2)));
        when(stockService.getStock(10L)).thenReturn(15);

        mockMvc.perform(get("/purchase-order/list-products").param("idFornecedor", idFornecedor.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/list-products - Deve retornar 500 em erro generico")
    void listProducts_ShouldReturn500_WhenError() throws Exception {
        when(productService.list(any(), any())).thenThrow(new RuntimeException());

        mockMvc.perform(get("/purchase-order/list-products").param("idFornecedor", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos."));
    }

    @Test
    @DisplayName("GET /purchase-order/list-purchase-order-products - Deve listar produtos do pedido")
    void listPurchaseOrderProducts_ShouldReturnOk() throws Exception {
        Long idPedido = 1L;
        StockDto stock = new StockDto();
        stock.setProduto(new ProductDto());
        stock.setQuantidade(10);

        when(stockService.getPurchaseOrderProducts(idPedido)).thenReturn(List.of(stock));

        mockMvc.perform(get("/purchase-order/list-purchase-order-products").param("idPedidoCompra", idPedido.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/list-purchase-order-products - Deve retornar 500 em erro generico")
    void listPurchaseOrderProducts_ShouldReturn500_WhenError() throws Exception {
        when(stockService.getPurchaseOrderProducts(1L)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/purchase-order/list-purchase-order-products").param("idPedidoCompra", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos."));
    }

    @Test
    @DisplayName("GET /purchase-order/find-by-order-number - Deve buscar por numero do pedido")
    void findByOrderNumber_ShouldReturnOk() throws Exception {
        String num = "PO-123";
        when(service.findByOrderNumber(num)).thenReturn(List.of(new PurchaseOrderDto()));

        mockMvc.perform(get("/purchase-order/find-by-order-number").param("numeroPedido", num))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchase-order/find-by-order-number - Deve retornar 400 em NoSuchElementException")
    void findByOrderNumber_ShouldReturn400_WhenNotFound() throws Exception {
        String num = "PO-999";
        when(service.findByOrderNumber(num)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/purchase-order/find-by-order-number").param("numeroPedido", num))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pedido de compra não encontrado: PO-999"));
    }
}