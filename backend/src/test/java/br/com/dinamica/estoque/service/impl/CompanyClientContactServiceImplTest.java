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
import br.com.dinamica.estoque.dto.CompanyClientContactDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.ContatoClienteEmpresa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.CompanyClientContactMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class CompanyClientContactServiceImplTest {

    @Mock
    private ContatoClienteEmpresaRepository repository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CompanyClientContactMapper modelMapper;

    private CompanyClientContactServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyClientContactServiceImpl(repository, clienteRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar o contato do cliente empresa por ID")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        ContatoClienteEmpresa entity = new ContatoClienteEmpresa();
        entity.setId(id);
        CompanyClientContactDto dto = new CompanyClientContactDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        CompanyClientContactDto result = service.get(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(Long idEmpresa, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina filtrada por idEmpresa e executar a lambda da Specification")
    void list_shouldReturnPageWithEmpresaFilter() {
        Long idEmpresa = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        ContatoClienteEmpresa entity = new ContatoClienteEmpresa();
        CompanyClientContactDto dto = new CompanyClientContactDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoClienteEmpresa> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<CompanyClientContactDto> result = service.list(idEmpresa, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem filtro quando idEmpresa for nulo")
    void list_shouldReturnPageWithoutFilterWhenIdEmpresaIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<ContatoClienteEmpresa> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<CompanyClientContactDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(CompanyClientContactDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve atualizar contato existente")
    void save_shouldUpdateExistingContact() {
        Long id = 1L;
        Long clienteId = 10L;

        CompanyClientContactDto dto = new CompanyClientContactDto();
        dto.setId(id);
        ClientDto clienteDto = new ClientDto();
        clienteDto.setId(clienteId);
        dto.setCliente(clienteDto);

        Usuario usuario = new Usuario();
        ContatoClienteEmpresa entityExistente = new ContatoClienteEmpresa();
        entityExistente.setId(id);
        Cliente cliente = new ClienteEmpresa();
        cliente.setId(clienteId);

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        CompanyClientContactDto result = service.save(dto, usuario);

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

        CompanyClientContactDto dto = new CompanyClientContactDto();
        dto.setId(null);
        ClientDto clienteDto = new ClientDto();
        clienteDto.setId(clienteId);
        dto.setCliente(clienteDto);

        Usuario usuario = new Usuario();
        Cliente cliente = new ClienteEmpresa();
        cliente.setId(clienteId);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(repository.save(any(ContatoClienteEmpresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(ContatoClienteEmpresa.class))).thenReturn(dto);

        CompanyClientContactDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(ContatoClienteEmpresa.class));
        verify(clienteRepository).findById(clienteId);
        verify(repository).save(any(ContatoClienteEmpresa.class));
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
    private void executeSpecification(Specification<ContatoClienteEmpresa> specification) {
        Root<ContatoClienteEmpresa> root = mock(Root.class);
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