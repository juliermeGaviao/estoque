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

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.PersonClientContactDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ClientePessoa;
import br.com.dinamica.estoque.entity.ContatoClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PersonClientContactMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class PersonClientContactServiceImplTest {

    @Mock
    private ContatoClientePessoaRepository repository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PersonClientContactMapper modelMapper;

    private PersonClientContactServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PersonClientContactServiceImpl(repository, clienteRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o contato do cliente pessoa por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        ContatoClientePessoa entity = new ContatoClientePessoa();
        entity.setId(id);
        PersonClientContactDto dto = new PersonClientContactDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        PersonClientContactDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(Long idPessoa, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina filtrada por idPessoa e executar a lambda da Specification")
    void list_shouldReturnPageWithPessoaFilter() {
        Long idPessoa = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        ContatoClientePessoa entity = new ContatoClientePessoa();
        PersonClientContactDto dto = new PersonClientContactDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoClientePessoa> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<PersonClientContactDto> result = service.list(idPessoa, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem filtro quando idPessoa for nulo")
    void list_shouldReturnPageWithoutFilterWhenIdPessoaIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoClientePessoa> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<PersonClientContactDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(PersonClientContactDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar contato existente")
    void save_shouldUpdateExistingContact() {
        Long id = 1L;
        Long clienteId = 10L;

        PersonClientContactDto dto = new PersonClientContactDto();
        dto.setId(id);
        ClientDto clienteDto = new ClientDto();
        clienteDto.setId(clienteId);
        dto.setCliente(clienteDto);

        Usuario usuario = new Usuario();
        ContatoClientePessoa entityExistente = new ContatoClientePessoa();
        entityExistente.setId(id);
        Cliente cliente = new ClientePessoa();
        cliente.setId(clienteId);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        PersonClientContactDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository).findById(id);
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(clienteRepository).findById(clienteId);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve criar novo contato quando ID for nulo")
    void save_shouldCreateNewContact() {
        Long clienteId = 10L;

        PersonClientContactDto dto = new PersonClientContactDto();
        dto.setId(null);
        ClientDto clienteDto = new ClientDto();
        clienteDto.setId(clienteId);
        dto.setCliente(clienteDto);

        Usuario usuario = new Usuario();
        Cliente cliente = new ClientePessoa();
        cliente.setId(clienteId);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(repository.save(any(ContatoClientePessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(ContatoClientePessoa.class))).thenReturn(dto);

        PersonClientContactDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(ContatoClientePessoa.class));
        verify(clienteRepository).findById(clienteId);
        verify(repository).save(any(ContatoClientePessoa.class));
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
    private void executeSpecification(Specification<ContatoClientePessoa> specification) {
        Root<ContatoClientePessoa> root = mock(Root.class);
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