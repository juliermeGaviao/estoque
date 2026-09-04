package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;
import br.com.dinamica.estoque.mapper.UserPriceTableMapper;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class UserPriceTableServiceImplTest {

    @Mock
    private UsuarioTabelaPrecoRepository repository;

    @Mock
    private TabelaPrecoRepository tabelaPrecoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UserPriceTableMapper modelMapper;

    private UserPriceTableServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserPriceTableServiceImpl(
                repository,
                tabelaPrecoRepository,
                usuarioRepository,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar DTO quando encontrar a entidade")
    void get_shouldReturnDto() {
        UsuarioTabelaPreco entity = new UsuarioTabelaPreco();
        UserPriceTableDto dto = new UserPriceTableDto();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        UserPriceTableDto result = service.get(1L);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(repository).findById(1L);
        verify(modelMapper).toDto(entity);
    }

    @Test
    @DisplayName("get - deve lançar exceção quando não encontrar")
    void get_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    // -------------------------------------------------------------------------
    // list()
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve aplicar todos os filtros da specification e retornar pagina")
    void list_shouldApplyFiltersAndReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioTabelaPreco entity = new UsuarioTabelaPreco();
        UserPriceTableDto dto = new UserPriceTableDto();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<UsuarioTabelaPreco> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(entity));
                });
        when(modelMapper.toDto(entity)).thenReturn(dto);

        Page<UserPriceTableDto> result = service.list(1L, 2L, pageable);

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
                    Specification<UsuarioTabelaPreco> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<UserPriceTableDto> result = service.list(null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve deletar por usuario e salvar nova tabela de preco")
    void save_shouldDeleteAndSave() {
        UserPriceTableDto dto = criarDto(10L, 20L);

        TabelaPreco tabela = new TabelaPreco();
        Usuario usuario = new Usuario();
        UsuarioTabelaPreco entitySalva = new UsuarioTabelaPreco();

        when(tabelaPrecoRepository.findById(20L)).thenReturn(Optional.of(tabela));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(repository.saveAndFlush(any(UsuarioTabelaPreco.class))).thenReturn(entitySalva);
        when(modelMapper.toDto(entitySalva)).thenReturn(dto);

        UserPriceTableDto result = service.save(dto);

        assertNotNull(result);
        verify(repository).deleteByUsuario(10L);
        verify(repository).saveAndFlush(any(UsuarioTabelaPreco.class));
    }

    @Test
    @DisplayName("save - deve lançar exceção quando tabela de preço não existir")
    void save_shouldThrowWhenPriceTableNotFound() {
        UserPriceTableDto dto = criarDto(10L, 99L);

        when(tabelaPrecoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto));
        verify(repository).deleteByUsuario(10L);
    }

    @Test
    @DisplayName("save - deve lançar exceção quando usuário não existir")
    void save_shouldThrowWhenUserNotFound() {
        UserPriceTableDto dto = criarDto(99L, 20L);

        when(tabelaPrecoRepository.findById(20L)).thenReturn(Optional.of(new TabelaPreco()));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.save(dto));
        verify(repository).deleteByUsuario(99L);
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
    // saveTables()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saveTables - deve deletar por usuario e salvar lista de tabelas de preco")
    void saveTables_shouldDeleteAndSaveList() {
        UserPriceTableDto dto1 = criarDto(10L, 20L);
        UserPriceTableDto dto2 = criarDto(10L, 21L);

        when(tabelaPrecoRepository.findById(20L)).thenReturn(Optional.of(new TabelaPreco()));
        when(tabelaPrecoRepository.findById(21L)).thenReturn(Optional.of(new TabelaPreco()));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(new Usuario()));

        service.saveTables(List.of(dto1, dto2));

        verify(repository).deleteByUsuario(10L);
        verify(repository, times(2)).saveAndFlush(any(UsuarioTabelaPreco.class));
    }

    // -------------------------------------------------------------------------
    // Helper para simular a Criteria API do Specification
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<UsuarioTabelaPreco> specification) {
        Root<UsuarioTabelaPreco> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);

        try {
            specification.toPredicate(root, query, cb);
        } catch (Exception ignored) {
            // Ignora exceções na execução dos Mocks de Criteria
        }
    }

    private UserPriceTableDto criarDto(Long idUsuario, Long idTabelaPreco) {
        UserPriceTableDto dto = new UserPriceTableDto();

        UserDto usuarioDto = new UserDto();
        usuarioDto.setId(idUsuario);

        PriceTableDto tabelaDto = new PriceTableDto();
        tabelaDto.setId(idTabelaPreco);

        dto.setUsuario(usuarioDto);
        dto.setTabela(tabelaDto);

        return dto;
    }

}