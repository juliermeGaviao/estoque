package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.com.dinamica.estoque.configuration.auth.SHA256PasswordEncoder;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserListDto;
import br.com.dinamica.estoque.dto.UserRequestDTO;
import br.com.dinamica.estoque.entity.Perfil;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;
import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;
import br.com.dinamica.estoque.mapper.UserMapper;
import br.com.dinamica.estoque.repository.PerfilRepository;
import br.com.dinamica.estoque.repository.UsuarioPontoVendaRepository;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.repository.UsuarioTabelaPrecoRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioTabelaPrecoRepository usuarioTabelaPrecoRepository;

    @Mock
    private UsuarioPontoVendaRepository usuarioPontoVendaRepository;

    @Mock
    private SHA256PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper modelMapper;

    @InjectMocks
    private UserServiceImpl service; // Ou a classe concreta correspondente

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                usuarioRepository,
                perfilRepository,
                usuarioTabelaPrecoRepository,
                usuarioPontoVendaRepository,
                passwordEncoder,
                modelMapper
        );
    }

    // -------------------------------------------------------------------------
    // loadUserByUsername(String email)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar UserDetails quando o e-mail for encontrado")
    void loadUserByUsername_QuandoEmailExiste_DeveRetornarUserDetails() {
        String email = "usuario@email.com";
        Usuario usuarioEsperado = new Usuario(); // Substitua pela sua classe de entidade/UserDetails
        
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioEsperado));

        UserDetails resultado = service.loadUserByUsername(email);

        assertNotNull(resultado);
        assertEquals(usuarioEsperado, resultado);
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException quando o e-mail não for encontrado")
    void loadUserByUsername_QuandoEmailNaoExiste_DeveLancarExcecao() {
        String email = "naoexistente@email.com";
        
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(email));

        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    // -------------------------------------------------------------------------
    // getUser(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUser - deve retornar DTO quando encontrar usuario por ID")
    void getUser_shouldReturnDtoWhenFound() {
        Long id = 1L;
        Usuario entity = new Usuario();
        entity.setId(id);
        UserDto dto = new UserDto();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(entity));
        when(modelMapper.toDto(entity)).thenReturn(dto);

        UserDto result = service.getUser(id);

        assertNotNull(result);
        assertEquals(dto, result);
        verify(usuarioRepository).findById(id);
        verify(modelMapper).toDto(entity);
    }

    // -------------------------------------------------------------------------
    // list(String email, Pageable pageable)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve mapear corretamente Usuario para UserListDto ordenando perfis, tabelas e pontos")
    void list_shouldMapUserToUserListDtoWithSortedAssociations() {
        Pageable pageable = PageRequest.of(0, 10);
        String emailFilter = "teste";

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setEmail("teste@dinamica.com");

        Perfil perfil1 = new Perfil();
        perfil1.setNome("ADMIN");
        Perfil perfil2 = new Perfil();
        perfil2.setNome("OPERADOR");
        usuario.setPerfis(java.util.Set.of(perfil2, perfil1));

        TabelaPreco tabela1 = new TabelaPreco();
        tabela1.setNome("Varejo");
        TabelaPreco tabela2 = new TabelaPreco();
        tabela2.setNome("Atacado");
        UsuarioTabelaPreco utp1 = new UsuarioTabelaPreco();
        utp1.setTabela(tabela1);
        UsuarioTabelaPreco utp2 = new UsuarioTabelaPreco();
        utp2.setTabela(tabela2);

        PontoVenda ponto1 = new PontoVenda();
        ponto1.setNome("Loja 02");
        PontoVenda ponto2 = new PontoVenda();
        ponto2.setNome("Loja 01");
        UsuarioPontoVenda upv1 = new UsuarioPontoVenda();
        upv1.setPontoVenda(ponto1);
        UsuarioPontoVenda upv2 = new UsuarioPontoVenda();
        upv2.setPontoVenda(ponto2);

        when(usuarioRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Usuario> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of(usuario));
                });
        when(usuarioTabelaPrecoRepository.findByUsuario(2L)).thenReturn(List.of(utp1, utp2));
        when(usuarioPontoVendaRepository.findByUsuario(2L)).thenReturn(List.of(upv1, upv2));

        Page<UserListDto> result = service.list(emailFilter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        UserListDto dto = result.getContent().get(0);
        assertEquals(2L, dto.getId());
        assertEquals("teste@dinamica.com", dto.getEmail());
        assertEquals("ADMIN, OPERADOR", dto.getPerfis());
        assertEquals("Atacado, Varejo", dto.getTabelas());
        assertEquals("Loja 01, Loja 02", dto.getPontos());
    }

    @SuppressWarnings("unchecked")
	@ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("list - deve retornar pagina sem filtro quando email for vazio ou em branco")
    void list_shouldReturnPageWithoutEmailFilterWhenBlank(String email) {
        Pageable pageable = PageRequest.of(0, 10);

        when(usuarioRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Usuario> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<UserListDto> result = service.list(email, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list - deve retornar pagina sem filtro quando email for nulo")
    void list_shouldReturnPageWithoutEmailFilterWhenNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(usuarioRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Usuario> spec = invocation.getArgument(0);
                    executeSpecification(spec);
                    return new PageImpl<>(List.of());
                });

        Page<UserListDto> result = service.list(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // save(UserRequestDTO dto, Usuario usuarioLogado)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve criar novo usuario com senha criptografada e perfis validos")
    void save_shouldCreateNewUser() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(null);
        dto.setEmail("novo@dinamica.com");
        dto.setSenha("123456");
        dto.setPerfis(List.of(1L, 2L));

        Usuario cadastrante = new Usuario();
        Perfil perfil1 = new Perfil();
        Perfil perfil2 = new Perfil();

        UserDto expectedDto = new UserDto();

        when(usuarioRepository.findByEmail("novo@dinamica.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("hash123456");
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(perfil1));
        when(perfilRepository.findById(2L)).thenReturn(Optional.of(perfil2));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toDto(any(Usuario.class))).thenReturn(expectedDto);

        UserDto result = service.save(dto, cadastrante);

        assertNotNull(result);
        verify(usuarioRepository, never()).findById(any());
        verify(passwordEncoder).encode("123456");
        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
    }

    @Test
    @DisplayName("save - deve atualizar usuario existente mantendo senha original se senha do DTO for nula")
    void save_shouldUpdateExistingUserWithoutChangingPasswordWhenSenhaIsNull() {
        Long id = 10L;
        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(id);
        dto.setEmail("existente@dinamica.com");
        dto.setSenha(null);
        dto.setPerfis(List.of(1L, 99L)); // 99L vai retornar Optional.empty() para testar o filter

        Usuario cadastrante = new Usuario();
        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(id);
        usuarioExistente.setSenha("senhaAntigaHash");

        Perfil perfil1 = new Perfil();
        UserDto expectedDto = new UserDto();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioExistente));
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(perfil1));
        when(perfilRepository.findById(99L)).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(usuarioExistente)).thenReturn(usuarioExistente);
        when(modelMapper.toDto(usuarioExistente)).thenReturn(expectedDto);

        UserDto result = service.save(dto, cadastrante);

        assertNotNull(result);
        assertEquals("senhaAntigaHash", usuarioExistente.getSenha());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository).saveAndFlush(usuarioExistente);
    }

    @Test
    void save_shouldThrowConstraintViolationException_whenEmailAlreadyExists() {
        // Arrange
        String email = "usuario@teste.com";

        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(null);
        dto.setEmail(email);

        Usuario outroUsuario = new Usuario();
        outroUsuario.setEmail(email);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(outroUsuario));

        assertThrows(ConstraintViolationException.class, () -> service.save(dto, new Usuario()));

        verify(usuarioRepository).findByEmail(email);
    }

    // -------------------------------------------------------------------------
    // changePassword(UserRequestDTO dto)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("changePassword - deve alterar a senha do usuario e retornar DTO")
    void changePassword_shouldUpdatePassword() {
        Long id = 1L;
        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(id);
        dto.setSenha("novaSenha123");

        Usuario usuario = new Usuario();
        usuario.setId(id);

        UserDto expectedDto = new UserDto();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novaSenhaHash");
        when(modelMapper.toDto(usuario)).thenReturn(expectedDto);

        UserDto result = service.changePassword(dto);

        assertNotNull(result);
        assertEquals("novaSenhaHash", usuario.getSenha());
        verify(usuarioRepository).findById(id);
        verify(passwordEncoder).encode("novaSenha123");
    }

    // -------------------------------------------------------------------------
    // delete(Long id)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve desativar o usuario ao inves de remover do banco")
    void delete_shouldDeactivateUser() {
        Long id = 1L;
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setAtivo(true);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        service.delete(id);

        assertFalse(usuario.getAtivo());
        verify(usuarioRepository).findById(id);
        verify(usuarioRepository).save(usuario);
    }

    // -------------------------------------------------------------------------
    // Helper para exercitar as especificações JPA/Criteria
    // -------------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<Usuario> specification) {
        Root<Usuario> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Expression stringExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(cb.lower(any())).thenReturn(stringExpression);
        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class))).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.notEqual(any(), any())).thenReturn(predicate);
        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);

        specification.toPredicate(root, query, cb);
    }
}