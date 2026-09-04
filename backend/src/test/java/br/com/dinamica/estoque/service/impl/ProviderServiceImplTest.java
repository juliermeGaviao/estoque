package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProviderMapper;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    private FornecedorRepository repository;

    @Mock
    private ContatoFornecedorRepository contatoFornecedorRepository;

    @Mock
    private ProviderMapper modelMapper;

    private ProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProviderServiceImpl(repository, contatoFornecedorRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o fornecedor por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        Fornecedor entity = new Fornecedor();
        entity.setId(id);
        ProviderDto dto = new ProviderDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        ProviderDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros quando informados e executar Specification")
    void list_shouldApplyAllFiltersWhenProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Fornecedor entity = new Fornecedor();
        ProviderDto dto = new ProviderDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Fornecedor> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<ProviderDto> result = service.list("Empresa LTDA", "Empresa", "12345678000199", "51999998888", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem aplicar filtros quando parametros forem nulos")
    void list_shouldReturnPageWithoutFiltersWhenParamsAreNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Fornecedor> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProviderDto> result = service.list(null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve ignorar filtros quando as strings forem vazias ou em branco")
    void list_shouldIgnoreFiltersWhenStringsAreBlank(String blankParam) {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Fornecedor> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProviderDto> result = service.list(blankParam, blankParam, blankParam, blankParam, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(ProviderDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar fornecedor existente")
    void save_shouldUpdateExistingProvider() {
        Long id = 1L;
        ProviderDto dto = new ProviderDto();
        dto.setId(id);

        Usuario usuario = new Usuario();
        Fornecedor entityExistente = new Fornecedor();
        entityExistente.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        ProviderDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo fornecedor quando ID for nulo")
    void save_shouldCreateNewProvider() {
        ProviderDto dto = new ProviderDto();
        dto.setId(null);

        Usuario usuario = new Usuario();

        when(repository.save(any(Fornecedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(Fornecedor.class))).thenReturn(dto);

        ProviderDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(Fornecedor.class));
        verify(repository).save(any(Fornecedor.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve excluir contatos associados e remover o fornecedor")
    void delete_shouldDeleteContactsAndProvider() {
        Long id = 1L;

        service.delete(id);

        verify(contatoFornecedorRepository).deleteByFornecedor_Id(id);
        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<Fornecedor> specification) {
        Root<Fornecedor> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Expression stringExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(stringExpression);
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);

        specification.toPredicate(root, query, cb);
    }
}