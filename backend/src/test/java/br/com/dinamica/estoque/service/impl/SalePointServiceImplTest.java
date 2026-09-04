package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.SalePointMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class SalePointServiceImplTest {

    @Mock
    private PontoVendaRepository repository;

    @Mock
    private ClienteRepository clientRepository;

    @Mock
    private SalePointMapper modelMapper;

    private SalePointServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SalePointServiceImpl(repository, clientRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar o DTO quando o registro for encontrado")
    void get_shouldReturnDtoWhenFound() {
        Long id = 1L;
        PontoVenda entity = new PontoVenda();
        SalePointDto expectedDto = new SalePointDto();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(expectedDto);

        SalePointDto result = service.get(id);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(repository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(String nome, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve filtrar por nome e retornar página com DTOs")
    void list_shouldFilterByNomeAndReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        PontoVenda entity = new PontoVenda();
        SalePointDto dto = new SalePointDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PontoVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<SalePointDto> result = service.list("Loja Centro", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve buscar sem filtro de nome se o nome for nulo ou em branco")
    void list_shouldSearchWithoutNomeFilterWhenNullOrBlank() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<PontoVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        // Testando com nome nulo
        Page<SalePointDto> resultNull = service.list(null, pageable);
        assertNotNull(resultNull);

        // Testando com nome em branco/espaços
        Page<SalePointDto> resultBlank = service.list("   ", pageable);
        assertNotNull(resultBlank);
    }

    // -------------------------------------------------------------------------
    // save(SalePointDto dto, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve criar novo ponto de venda com empresa vinculada")
    void save_shouldCreateNewSalePointWithEmpresa() {
        SalePointDto dto = new SalePointDto();
        CommonClientDto empresaDto = new CommonClientDto();
        empresaDto.setId(10L);
        dto.setEmpresa(empresaDto);

        Usuario usuario = new Usuario();
        ClienteEmpresa clienteEmpresa = new ClienteEmpresa();
        PontoVenda savedEntity = new PontoVenda();

        when(clientRepository.findById(10L)).thenReturn(Optional.of(clienteEmpresa));
        when(repository.save(any(PontoVenda.class))).thenReturn(savedEntity);
        when(modelMapper.toDto(savedEntity)).thenReturn(dto);

        SalePointDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(repository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(eq(dto), any(PontoVenda.class));
        verify(clientRepository).findById(10L);
        verify(repository).save(any(PontoVenda.class));
    }

    @Test
    @DisplayName("save - deve atualizar ponto de venda existente sem empresa (empresa nula/sem id)")
    void save_shouldUpdateExistingSalePointWithoutEmpresa() {
        Long id = 1L;
        SalePointDto dto = new SalePointDto();
        dto.setId(id);
        dto.setEmpresa(null); // Sem empresa vinculada

        Usuario usuario = new Usuario();
        PontoVenda entityExistente = new PontoVenda();

        when(repository.findById(id)).thenReturn(Optional.of(entityExistente));
        when(repository.save(entityExistente)).thenReturn(entityExistente);
        when(modelMapper.toDto(entityExistente)).thenReturn(dto);

        SalePointDto result = service.save(dto, usuario);

        assertNotNull(result);
        assertNull(entityExistente.getEmpresa());
        verify(repository).findById(id);
        verify(clientRepository, never()).findById(any());
        verify(modelMapper).updateEntityFromDto(dto, entityExistente);
        verify(repository).save(entityExistente);
    }

    @Test
    @DisplayName("save - deve anular empresa quando DTO possui empresa mas ID é nulo")
    void save_shouldSetEmpresaNullWhenEmpresaDtoHasNullId() {
        SalePointDto dto = new SalePointDto();
        CommonClientDto empresaDto = new CommonClientDto();
        empresaDto.setId(null); // DTO informado mas com ID nulo
        dto.setEmpresa(empresaDto);

        Usuario usuario = new Usuario();
        PontoVenda savedEntity = new PontoVenda();

        when(repository.save(any(PontoVenda.class))).thenReturn(savedEntity);
        when(modelMapper.toDto(savedEntity)).thenReturn(dto);

        SalePointDto result = service.save(dto, usuario);

        assertNotNull(result);
        verify(clientRepository, never()).findById(any());
        verify(repository).save(any(PontoVenda.class));
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve chamar repository.deleteById com o ID correto")
    void delete_shouldCallRepositoryDeleteById() {
        Long id = 5L;

        service.delete(id);

        verify(repository).deleteById(id);
    }

    // -------------------------------------------------------------------------
    // save(List<SalePointDto> dtos, Usuario usuario)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save (em lote) - deve processar todos os itens da lista chamando save unitário")
    void saveList_shouldProcessAllItemsInList() {
        SalePointDto dto1 = new SalePointDto();
        SalePointDto dto2 = new SalePointDto();
        List<SalePointDto> dtos = List.of(dto1, dto2);
        Usuario usuario = new Usuario();

        when(repository.save(any(PontoVenda.class))).thenAnswer(i -> i.getArgument(0));

        service.save(dtos, usuario);

        verify(repository, times(2)).save(any(PontoVenda.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<PontoVenda> specification) {
        Root<PontoVenda> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(path);

        specification.toPredicate(root, query, cb);
    }
}