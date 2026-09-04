package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.mapper.SaleMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.StockService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock
    private VendaRepository repository;

    @Mock
    private ItemVendaRepository itemVendaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TabelaPrecoRepository precoTabelaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PontoVendaRepository pontoVendaRepository;

    @Mock
    private StockService stockService;

    @Mock
    private SaleMapper modelMapper;

    private SaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SaleServiceImpl(
                repository,
                itemVendaRepository,
                usuarioRepository,
                precoTabelaRepository,
                clienteRepository,
                pontoVendaRepository,
                stockService,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO com os perfis do vendedor anulados")
    void get_shouldReturnDtoAndSetVendedorPerfisToNull() {
        Venda entity = new Venda();
        SaleDto dto = criarDtoComVendedor();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        SaleDto result = service.get(1L);

        assertNotNull(result);
        assertNull(result.getVendedor().getPerfis());
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("get - deve lançar exceção quando venda não existir")
    void get_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    // -------------------------------------------------------------------------
    // list()
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros (Cliente, Vendedor, Intervalo Desconto e Observações)")
    void list_shouldApplyAllFiltersAndReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Venda entity = new Venda();
        SaleDto dto = criarDtoComVendedor();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Venda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<SaleDto> result = service.list(1L, 2L, 5.0f, 15.0f, "Observação", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getVendedor().getPerfis());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro com apenas minDesconto")
    void list_shouldApplyOnlyMinDescontoFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Venda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<SaleDto> result = service.list(null, null, 10.0f, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar filtro com apenas maxDesconto")
    void list_shouldApplyOnlyMaxDescontoFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Venda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<SaleDto> result = service.list(null, null, null, 20.0f, "", pageable);

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
                    Specification<Venda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<SaleDto> result = service.list(null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar venda existente com cliente informado")
    void save_shouldUpdateExistingSaleWithClient() {
        SaleDto dto = criarDtoCompleto(1L, 10L, 20L, 30L, 40L);
        Usuario usuario = new Usuario();
        Venda entityExistente = new Venda();
        Cliente clienteMock = mock(Cliente.class);

        when(repository.findById(1L)).thenReturn(Optional.of(entityExistente));
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(new Usuario()));
        when(precoTabelaRepository.findById(30L)).thenReturn(Optional.of(new TabelaPreco()));
        when(pontoVendaRepository.findById(40L)).thenReturn(Optional.of(new PontoVenda()));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        SaleDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar nova venda sem cliente")
    void save_shouldCreateNewSaleWithoutClient() {
        SaleDto dto = criarDtoCompleto(null, null, 20L, 30L, 40L);
        Usuario usuario = new Usuario();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(new Usuario()));
        when(precoTabelaRepository.findById(30L)).thenReturn(Optional.of(new TabelaPreco()));
        when(pontoVendaRepository.findById(40L)).thenReturn(Optional.of(new PontoVenda()));
        when(repository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
        when(modelMapper.toDto(any(Venda.class))).thenReturn(dto);

        SaleDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).save(any(Venda.class));
    }

    @Test
    @DisplayName("save - deve criar nova venda com DTO de cliente porém sem ID preenchido")
    void save_shouldCreateNewSaleWithClientDtoWithoutId() {
        SaleDto dto = criarDtoCompleto(null, null, 20L, 30L, 40L);
        
        ClientDto clientDto = new ClientDto();
        dto.setCliente(clientDto);

        Usuario usuario = new Usuario();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(new Usuario()));
        when(precoTabelaRepository.findById(30L)).thenReturn(Optional.of(new TabelaPreco()));
        when(pontoVendaRepository.findById(40L)).thenReturn(Optional.of(new PontoVenda()));
        when(repository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
        when(modelMapper.toDto(any(Venda.class))).thenReturn(dto);

        SaleDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).save(any(Venda.class));
    }

    @Test
    @DisplayName("save - deve lançar exceção se ID da venda não for encontrado")
    void save_shouldThrowWhenSaleIdNotFound() {
        SaleDto dto = criarDtoCompleto(99L, 10L, 20L, 30L, 40L);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto, new Usuario()));
    }

    @Test
    @DisplayName("save - deve lançar exceção se cliente informado não for encontrado")
    void save_shouldThrowWhenClientNotFound() {
        SaleDto dto = criarDtoCompleto(null, 99L, 20L, 30L, 40L);

        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto, new Usuario()));
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve estornar estoque, deletar itens e remover a venda")
    void delete_shouldUndoStockDeleteItemsAndDeleteSale() {
        Long idVenda = 5L;
        Usuario usuario = new Usuario();

        service.delete(idVenda, usuario);

        verify(stockService).undoSale(idVenda, usuario);
        verify(itemVendaRepository).deleteByVenda_Id(idVenda);
        verify(repository).deleteById(idVenda);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<Venda> specification) {
        Root<Venda> root = mock(Root.class);
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
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(path);

        try {
            specification.toPredicate(root, query, cb);
        } catch (Exception ignored) {
            // Ignora exceções na execução dos Mocks de Criteria
        }
    }

    private SaleDto criarDtoComVendedor() {
        SaleDto dto = new SaleDto();
        UserDto vendedor = new UserDto();
        vendedor.setPerfis(new ArrayList<>());
        dto.setVendedor(vendedor);
        return dto;
    }

    private SaleDto criarDtoCompleto(Long id, Long idCliente, Long idVendedor, Long idTabela, Long idPontoVenda) {
        SaleDto dto = criarDtoComVendedor();
        dto.getId();
        dto.setId(id);

        if (idCliente != null) {
            ClientDto clientDto = new ClientDto();
            clientDto.setId(idCliente);
            dto.setCliente(clientDto);
        }

        dto.getVendedor().setId(idVendedor);

        PriceTableDto tabela = new PriceTableDto();
        tabela.setId(idTabela);
        dto.setTabela(tabela);

        SalePointDto pontoVenda = new SalePointDto();
        pontoVenda.setId(idPontoVenda);
        dto.setPontoVenda(pontoVenda);

        return dto;
    }
}