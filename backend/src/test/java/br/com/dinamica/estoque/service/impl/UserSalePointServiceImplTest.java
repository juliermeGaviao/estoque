package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;
import br.com.dinamica.estoque.mapper.UserSalePointMapper;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioPontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class UserSalePointServiceImplTest {

    @Mock
    private UsuarioPontoVendaRepository repository;

    @Mock
    private PontoVendaRepository pontoVendaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UserSalePointMapper modelMapper;

    private UserSalePointServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserSalePointServiceImpl(repository, pontoVendaRepository, usuarioRepository, modelMapper);
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar a entidade")
    void get_shouldReturnDto() {
        UsuarioPontoVenda entity = new UsuarioPontoVenda();
        UserSalePointDto dto = new UserSalePointDto();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        UserSalePointDto result = service.get(1L);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(1L);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list()
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros da specification e retornar pagina")
    void list_shouldApplyFiltersAndReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioPontoVenda entity = new UsuarioPontoVenda();
        UserSalePointDto dto = new UserSalePointDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<UsuarioPontoVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<UserSalePointDto> result = service.list(1L, 2L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(dto, result.getContent().get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve funcionar sem filtros")
    void list_shouldWorkWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<UsuarioPontoVenda> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<UserSalePointDto> result = service.list(null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve deletar por usuario e salvar novo ponto de venda")
    void save_shouldDeleteAndSave() {
        UserSalePointDto dto = criarDto(10L, 20L);

        Usuario usuario = new Usuario();
        PontoVenda pontoVenda = new PontoVenda();
        UsuarioPontoVenda entitySalva = new UsuarioPontoVenda();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(pontoVendaRepository.findById(20L)).thenReturn(Optional.of(pontoVenda));
        when(repository.saveAndFlush(any(UsuarioPontoVenda.class))).thenReturn(entitySalva);
        when(modelMapper.toDto(entitySalva)).thenReturn(dto);

        UserSalePointDto result = service.save(dto);

        assertNotNull(result);
        verify(repository).deleteByUsuario(10L);
        verify(repository).saveAndFlush(any(UsuarioPontoVenda.class));
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve remover por id")
    void delete_shouldDeleteById() {
        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    // -------------------------------------------------------------------------
    // saveSalePoints()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saveSalePoints - deve deletar por usuario e salvar lista de pontos de venda")
    void saveSalePoints_shouldDeleteAndSaveList() {
        UserSalePointDto dto1 = criarDto(10L, 20L);
        UserSalePointDto dto2 = criarDto(10L, 21L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(new Usuario()));
        when(pontoVendaRepository.findById(20L)).thenReturn(Optional.of(new PontoVenda()));
        when(pontoVendaRepository.findById(21L)).thenReturn(Optional.of(new PontoVenda()));

        service.saveSalePoints(List.of(dto1, dto2));

        verify(repository).deleteByUsuario(10L);
        verify(repository, times(2)).saveAndFlush(any(UsuarioPontoVenda.class));
    }

    // -------------------------------------------------------------------------
    // Helper para simular a Criteria API do Specification
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<UsuarioPontoVenda> specification) {
        Root<UsuarioPontoVenda> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);

        specification.toPredicate(root, query, cb);
    }

    private UserSalePointDto criarDto(Long idUsuario, Long idPontoVenda) {
        UserSalePointDto dto = new UserSalePointDto();

        UserDto usuarioDto = new UserDto();
        usuarioDto.setId(idUsuario);

        SalePointDto pontoVendaDto = new SalePointDto();
        pontoVendaDto.setId(idPontoVenda);

        dto.setUsuario(usuarioDto);
        dto.setPontoVenda(pontoVendaDto);

        return dto;
    }

}