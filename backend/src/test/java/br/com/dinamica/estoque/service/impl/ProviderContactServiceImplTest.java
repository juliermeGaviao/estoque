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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.ContatoFornecedor;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProviderContactMapper;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProviderContactServiceImplTest {

    @Mock
    private ContatoFornecedorRepository repository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ProviderContactMapper modelMapper;

    private ProviderContactServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProviderContactServiceImpl(repository, fornecedorRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o contato por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        ContatoFornecedor entity = new ContatoFornecedor();
        entity.setId(id);
        ProviderContactDto dto = new ProviderContactDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        ProviderContactDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(Long idFornecedor, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina filtrada por idFornecedor e executar a lambda da Specification")
    void list_shouldReturnPageWithFornecedorFilter() {
        Long idFornecedor = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        ContatoFornecedor entity = new ContatoFornecedor();
        ProviderContactDto dto = new ProviderContactDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoFornecedor> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<ProviderContactDto> result = service.list(idFornecedor, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem filtro quando idFornecedor for nulo")
    void list_shouldReturnPageWithoutFilterWhenIdFornecedorIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoFornecedor> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<ProviderContactDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(ProviderContactDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar contato existente")
    void save_shouldUpdateExistingContact() {
        Long id = 1L;
        Long fornecedorId = 10L;

        ProviderContactDto dto = new ProviderContactDto();
        dto.setId(id);
        ProviderDto fornecedorDto = new ProviderDto();
        fornecedorDto.setId(fornecedorId);
        dto.setFornecedor(fornecedorDto);

        Usuario usuario = new Usuario();
        ContatoFornecedor entityExistente = new ContatoFornecedor();
        entityExistente.setId(id);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(fornecedorId);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(fornecedorRepository.findById(fornecedorId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        ProviderContactDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(fornecedorRepository).findById(fornecedorId);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo contato quando ID for nulo")
    void save_shouldCreateNewContact() {
        Long fornecedorId = 10L;

        ProviderContactDto dto = new ProviderContactDto();
        dto.setId(null);
        ProviderDto fornecedorDto = new ProviderDto();
        fornecedorDto.setId(fornecedorId);
        dto.setFornecedor(fornecedorDto);

        Usuario usuario = new Usuario();
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(fornecedorId);

        when(fornecedorRepository.findById(fornecedorId)).thenReturn(Optional.of(fornecedor));
        when(repository.save(any(ContatoFornecedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(ContatoFornecedor.class))).thenReturn(dto);

        ProviderContactDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(ContatoFornecedor.class));
        verify(fornecedorRepository).findById(fornecedorId);
        verify(repository).save(any(ContatoFornecedor.class));
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
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<ContatoFornecedor> specification) {
        Root<ContatoFornecedor> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);


        specification.toPredicate(root, query, cb);
    }
}