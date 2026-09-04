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

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PriceTableMapper;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class PriceTableServiceImplTest {

    @Mock
    private TabelaPrecoRepository repository;

    @Mock
    private TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository;

    @Mock
    private UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository;

    @Mock
    private PriceTableMapper modelMapper;

    private PriceTableServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PriceTableServiceImpl(
                repository,
                tabelaPrecoProdutoRepository,
                usuarioTabelaPrecoRepository,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar a tabela de preço por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        TabelaPreco entity = new TabelaPreco();
        entity.setId(id);
        PriceTableDto dto = new PriceTableDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PriceTableDto result = service.get(id);

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
    @DisplayName("list - deve retornar página filtrada por nome e executar a lambda da Specification")
    void list_shouldReturnPageWithNameFilter() {
        String nome = "Atacado";
        Pageable pageable = PageRequest.of(0, 10);
        TabelaPreco entity = new TabelaPreco();
        PriceTableDto dto = new PriceTableDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TabelaPreco> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PriceTableDto> result = service.list(nome, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve retornar página sem filtro quando o nome for vazio ou em branco")
    void list_shouldReturnPageWithoutFilterWhenNameIsBlank(String nome) {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TabelaPreco> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PriceTableDto> result = service.list(nome, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar página sem filtro quando o nome for nulo")
    void list_shouldReturnPageWithoutFilterWhenNameIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<TabelaPreco> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PriceTableDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(PriceTableDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar tabela de preço existente")
    void save_shouldUpdateExistingPriceTable() {
        Long id = 1L;
        PriceTableDto dto = new PriceTableDto();
        dto.setId(id);

        Usuario usuario = new Usuario();
        TabelaPreco entityExistente = new TabelaPreco();
        entityExistente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        PriceTableDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar nova tabela de preço quando ID for nulo")
    void save_shouldCreateNewPriceTable() {
        PriceTableDto dto = new PriceTableDto();
        dto.setId(null);

        Usuario usuario = new Usuario();

        when(repository.save(any(TabelaPreco.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(TabelaPreco.class))).thenReturn(dto);

        PriceTableDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(TabelaPreco.class));
        verify(repository).save(any(TabelaPreco.class));
    }

    // -------------------------------------------------------------------------
    // save(List<PriceTableDto> dtos, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save list - deve iterar e salvar todos os DTOs da lista")
    void saveList_shouldSaveAllDtos() {
        PriceTableDto dto1 = new PriceTableDto();
        PriceTableDto dto2 = new PriceTableDto();
        List<PriceTableDto> dtos = List.of(dto1, dto2);
        Usuario usuario = new Usuario();

        when(repository.save(any(TabelaPreco.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(dtos, usuario);

        verify(repository, times(2)).save(any(TabelaPreco.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve remover relacionamentos em produto e usuario antes de excluir a tabela")
    void delete_shouldRemoveRelationsAndDeletePriceTable() {
        Long id = 1L;

        service.delete(id);

        verify(tabelaPrecoProdutoRepository).deleteByTabela_Id(id);
        verify(usuarioTabelaPrecoRepository).deleteByTabela_Id(id);
        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<TabelaPreco> specification) {
        Root<TabelaPreco> root = mock(Root.class);
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