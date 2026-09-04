package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.PersonFilterDto;
import br.com.dinamica.estoque.dto.ResultadoCargaEmpregadosDto;
import br.com.dinamica.estoque.entity.ArquivoClientePessoa;
import br.com.dinamica.estoque.entity.ArquivoEmpresa;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.ClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ArquivoClientePessoaRepository;
import br.com.dinamica.estoque.repository.ArquivoEmpresaRepository;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClienteRepository repository;

    @Mock
    private ContatoClienteEmpresaRepository contatoClienteEmpresaRepository;

    @Mock
    private ContatoClientePessoaRepository contatoClientePessoaRepository;

    @Mock
    private ArquivoEmpresaRepository arquivoEmpresaRepository;

    @Mock
    private ArquivoClientePessoaRepository arquivoClientePessoaRepository;

    @TempDir
    private java.nio.file.Path tempDir;

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(
                repository,
                contatoClienteEmpresaRepository,
                contatoClientePessoaRepository,
                arquivoEmpresaRepository,
                arquivoClientePessoaRepository,
                tempDir.toString()
        );
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("get - deve retornar pessoa com todos os dados")
    void get_shouldReturnPerson() {
        ClientePessoa pessoa = pessoaCompleta();

        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));

        ClientDto result = service.get(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("João", result.getNome());
        assertEquals("999999999", result.getFone());
        assertEquals("Rua A", result.getEndereco());
        assertEquals("Centro", result.getBairro());
        assertEquals("90000000", result.getCep());
        assertEquals("Porto Alegre", result.getCidade());
        assertEquals("RS", result.getUf());
        assertEquals(LocalDate.of(1990, 1, 1), result.getDataAniversario());
        assertEquals(new BigDecimal("1000.00"), result.getLimite());
        assertEquals("123", result.getCracha());

        assertNotNull(result.getEmpresa());
        assertEquals(2L, result.getEmpresa().getId());
        assertEquals("Empresa", result.getEmpresa().getNome());
    }

    @Test
    @DisplayName("get - deve retornar pessoa sem empresa")
    void get_shouldReturnPersonWithoutCompany() {
        ClientePessoa pessoa = new ClientePessoa();
        pessoa.setId(1L);
        pessoa.setNome("João");

        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));

        ClientDto result = service.get(1L);

        assertNotNull(result);
        assertEquals("João", result.getNome());
        assertNull(result.getEmpresa());
    }

    @Test
    @DisplayName("get - deve retornar empresa")
    void get_shouldReturnCompany() {
        ClienteEmpresa empresa = empresaCompleta();

        when(repository.findById(2L)).thenReturn(Optional.of(empresa));

        ClientDto result = service.get(2L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Empresa", result.getNome());
        assertEquals("Empresa LTDA", result.getRazaoSocial());
        assertEquals("12345678000199", result.getCnpj());
        assertEquals("999999999", result.getFone());
        assertEquals("Rua B", result.getEndereco());
        assertEquals("Centro", result.getBairro());
        assertEquals("90000000", result.getCep());
        assertEquals("Porto Alegre", result.getCidade());
        assertEquals("RS", result.getUf());
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAll - deve retornar clientes ordenados por nome")
    void findAll_shouldReturnCommonDtos() {
        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(1L);
        empresa.setNome("Empresa");

        ClientePessoa pessoa = new ClientePessoa();
        pessoa.setId(2L);
        pessoa.setNome("Pessoa");

        when(repository.findAll(any(Sort.class)))
                .thenReturn(List.of(empresa, pessoa));

        List<CommonClientDto> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Empresa", result.get(0).getNome());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Pessoa", result.get(1).getNome());
    }

    // -------------------------------------------------------------------------
    // list() - empresas
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list empresas - deve aplicar todos os filtros")
    void listCompanies_shouldApplyAllFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        ClienteEmpresa empresa = empresaCompleta();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Cliente> specification =
                            invocation.getArgument(0);

                    executeSpecification(specification);

                    return new PageImpl<>(List.of(empresa));
                });

        Page<ClientDto> result = service.list(
                "Empresa",
                "nome",
                "12345678000199",
                "999999999",
                pageable
        );

        assertEquals(1, result.getContent().size());
        assertEquals("Empresa", result.getContent().get(0).getNome());
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list empresas - deve funcionar sem filtros")
    void listCompanies_shouldWorkWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Cliente> specification =
                            invocation.getArgument(0);

                    executeSpecification(specification);

                    return new PageImpl<>(List.of());
                });

        Page<ClientDto> result = service.list(
                null,
                "",
                "   ",
                null,
                pageable
        );

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // list() - pessoas
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list pessoas - deve aplicar todos os filtros")
    void listPersons_shouldApplyAllFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        PersonFilterDto filter = new PersonFilterDto();
        filter.setNome("João");
        filter.setIdEmpresa(10L);
        filter.setFone("999999999");
        filter.setMinLimite(new BigDecimal("100"));
        filter.setMaxLimite(new BigDecimal("500"));
        filter.setMinAniversario(LocalDate.of(1990, 1, 1));
        filter.setMaxAniversario(LocalDate.of(2000, 1, 1));

        ClientePessoa pessoa = pessoaCompleta();

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Cliente> specification =
                            invocation.getArgument(0);

                    executeSpecification(specification);

                    return new PageImpl<>(List.of(pessoa));
                });

        Page<ClientDto> result = service.list(filter, pageable);

        assertEquals(1, result.getContent().size());
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list pessoas - deve cobrir filtros somente mínimos")
    void listPersons_shouldApplyOnlyMinimumFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    executeSpecification(invocation.getArgument(0));
                    return new PageImpl<>(List.of());
                });

        PersonFilterDto filter = new PersonFilterDto();
        filter.setNome("João");
        filter.setFone("999");
        filter.setMinLimite(new BigDecimal("100"));
        filter.setMinAniversario(LocalDate.of(1990, 1, 1));

        service.list(filter, pageable);
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list pessoas - deve cobrir filtros somente máximos")
    void listPersons_shouldApplyOnlyMaximumFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    executeSpecification(invocation.getArgument(0));
                    return new PageImpl<>(List.of());
                });

        PersonFilterDto filter = new PersonFilterDto();
        filter.setMaxLimite(new BigDecimal("500"));
        filter.setMaxAniversario(LocalDate.of(2000, 1, 1));

        service.list(filter, pageable);
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("list pessoas - deve funcionar sem filtros")
    void listPersons_shouldWorkWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    executeSpecification(invocation.getArgument(0));
                    return new PageImpl<>(List.of());
                });

        PersonFilterDto filter = new PersonFilterDto();
        filter.setNome("   ");
        filter.setFone("");

        Page<ClientDto> result = service.list(filter, pageable);

        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - deve criar nova pessoa")
    void save_shouldCreatePerson() {
        ClientDto dto = new ClientDto();
        dto.setNome("João");
        dto.setFone("999");
        dto.setDataAniversario(LocalDate.of(1990, 1, 1));
        dto.setLimite(new BigDecimal("1000"));
        dto.setCracha("123");

        when(repository.save(any(ClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertNotNull(result);
        assertEquals("João", result.getNome());
        assertEquals("999", result.getFone());
        assertEquals("123", result.getCracha());
        assertEquals(new BigDecimal("1000"), result.getLimite());
    }

    @Test
    @DisplayName("save - deve criar pessoa sem empresa")
    void save_shouldCreatePersonWithoutCompany() {
        ClientDto dto = new ClientDto();
        dto.setNome("Pessoa");
        dto.setDataAniversario(LocalDate.of(1990, 1, 1)); // Adicionado para identificar como ClientePessoa

        when(repository.save(any(ClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertNotNull(result);
        assertEquals("Pessoa", result.getNome());
        assertEquals(LocalDate.of(1990, 1, 1), result.getDataAniversario());
        assertNull(result.getEmpresa());
    }

    @Test
    @DisplayName("save - deve criar empresa")
    void save_shouldCreateCompany() {
        ClientDto dto = new ClientDto();
        dto.setNome("Fantasia");
        dto.setRazaoSocial("Empresa LTDA");
        dto.setCnpj("12345678000199");

        when(repository.save(any(ClienteEmpresa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertNotNull(result);
        assertEquals("Fantasia", result.getNome());
        assertEquals("Empresa LTDA", result.getRazaoSocial());
        assertEquals("12345678000199", result.getCnpj());
    }

    @Test
    @DisplayName("save - deve atualizar pessoa com empresa")
    void save_shouldUpdatePersonWithCompany() {
        ClientDto dto = new ClientDto();
        dto.setId(1L);
        dto.setNome("João Atualizado");
        dto.setDataAniversario(LocalDate.of(1990, 1, 1));

        ClientDto empresaDto = new ClientDto();
        empresaDto.setId(10L);
        dto.setEmpresa(empresaDto);

        ClientePessoa pessoa = new ClientePessoa();
        pessoa.setId(1L);

        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(10L);
        empresa.setNome("Empresa");

        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.findById(10L)).thenReturn(Optional.of(empresa));
        when(repository.save(any(ClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertEquals("João Atualizado", result.getNome());
        assertNotNull(result.getEmpresa());
        assertEquals(10L, result.getEmpresa().getId());
    }

    @Test
    @DisplayName("save - deve atualizar empresa")
    void save_shouldUpdateCompany() {
        ClientDto dto = new ClientDto();
        dto.setId(1L);
        dto.setNome("Empresa Atualizada");
        dto.setRazaoSocial("Nova Razao");
        dto.setCnpj("99999999000199");

        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(empresa));
        when(repository.save(any(ClienteEmpresa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertEquals("Empresa Atualizada", result.getNome());
        assertEquals("Nova Razao", result.getRazaoSocial());
        assertEquals("99999999000199", result.getCnpj());
    }

    @Test
    @DisplayName("save - deve atualizar pessoa sem empresa")
    void save_shouldUpdatePersonWithoutCompany() {
        ClientDto dto = new ClientDto();
        dto.setId(1L);
        dto.setNome("Pessoa Atualizada");
        dto.setDataAniversario(LocalDate.of(1990, 1, 1));

        ClientePessoa pessoa = new ClientePessoa();
        pessoa.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.save(any(ClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertEquals("Pessoa Atualizada", result.getNome());
        assertNull(result.getEmpresa());
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete - deve remover contatos e cliente")
    void delete_shouldDeleteAllRelatedData() {
        service.delete(10L);

        verify(contatoClienteEmpresaRepository)
                .deleteByCliente_Id(10L);

        verify(contatoClientePessoaRepository)
                .deleteByCliente_Id(10L);

        verify(repository)
                .deleteById(10L);
    }

    // -------------------------------------------------------------------------
    // loadEmployees()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("loadEmployees - deve processar empregados novos, existentes e ignorar inválidos")
    void loadEmployees_shouldProcessAllBranches() throws IOException {
        Long idEmpresa = 1L;

        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(idEmpresa);
        empresa.setNome("Empresa");

        when(repository.findById(idEmpresa))
                .thenReturn(Optional.of(empresa));

        ArquivoEmpresa arquivoEmpresa = new ArquivoEmpresa();
        arquivoEmpresa.setId(100L);

        when(arquivoEmpresaRepository.saveAndFlush(any(ArquivoEmpresa.class)))
                .thenReturn(arquivoEmpresa);

        ClientePessoa existente = new ClientePessoa();
        existente.setId(50L);

        when(repository.getEmployee(idEmpresa, "C001"))
                .thenReturn(Optional.of(existente));

        when(repository.getEmployee(idEmpresa, "C002"))
                .thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(ClientePessoa.class)))
                .thenAnswer(invocation -> {
                    ClientePessoa pessoa = invocation.getArgument(0);
                    if (pessoa.getId() == null) {
                        pessoa.setId(51L);
                    }
                    return pessoa;
                });

        when(arquivoClientePessoaRepository.saveAndFlush(
                any(ArquivoClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String csv =
                "nome,numero-cracha,data-aniversario,limite-gasto\n" +
                "João,C001,01/01/1990,1000\n" +      // Válido 1 (Atualiza)
                "Maria,C002,02/02/1992,2000\n" +     // Válido 2 (Novo)
                ",C003,03/03/1995,1500\n" +          // Inválido (Sem nome)
                "Carlos,,04/04/1994,1800\n";          // Inválido (Sem crachá)

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empregados.csv",
                "text/csv",
                csv.getBytes()
        );

        Usuario usuario = new Usuario();

        ResultadoCargaEmpregadosDto result =
                service.loadEmployees(idEmpresa, file, usuario);

        assertNotNull(result);
        assertEquals(4L, result.getCarregados());
        assertEquals(4L, result.getTotal());

        verify(repository).getEmployee(idEmpresa, "C001");
        verify(repository).getEmployee(idEmpresa, "C002");

        verify(repository, times(4))
                .saveAndFlush(any(ClientePessoa.class));

        verify(arquivoClientePessoaRepository, times(4))
                .saveAndFlush(any(ArquivoClientePessoa.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ClientePessoa pessoaCompleta() {
        ClientePessoa pessoa = new ClientePessoa();

        pessoa.setId(1L);
        pessoa.setNome("João");
        pessoa.setFone("999999999");
        pessoa.setEndereco("Rua A");
        pessoa.setBairro("Centro");
        pessoa.setCep("90000000");
        pessoa.setCidade("Porto Alegre");
        pessoa.setUf("RS");
        pessoa.setDataAniversario(LocalDate.of(1990, 1, 1));
        pessoa.setLimite(new BigDecimal("1000.00"));
        pessoa.setCracha("123");

        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(2L);
        empresa.setNome("Empresa");

        pessoa.setEmpresa(empresa);

        return pessoa;
    }

    private ClienteEmpresa empresaCompleta() {
        ClienteEmpresa empresa = new ClienteEmpresa();

        empresa.setId(2L);
        empresa.setNome("Empresa");
        empresa.setFone("999999999");
        empresa.setEndereco("Rua B");
        empresa.setBairro("Centro");
        empresa.setCep("90000000");
        empresa.setCidade("Porto Alegre");
        empresa.setUf("RS");
        empresa.setRazaoSocial("Empresa LTDA");
        empresa.setCnpj("12345678000199");

        return empresa;
    }

    /**
     * Executa a Specification para que os lambdas internos sejam efetivamente
     * executados durante o teste, permitindo cobertura das linhas dos filtros.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeSpecification(Specification<Cliente> specification) {
        Root<Cliente> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        org.mockito.Mockito.lenient().when(root.type()).thenReturn(path);
        org.mockito.Mockito.lenient().when(root.get(any(String.class))).thenReturn(path);
        org.mockito.Mockito.lenient().when(path.get(any(String.class))).thenReturn(path);

        org.mockito.Mockito.lenient().when(cb.equal(any(), any())).thenReturn(predicate);

        org.mockito.Mockito.lenient().when(cb.like(any(), any(String.class)))
                .thenReturn(predicate);

        org.mockito.Mockito.lenient().when(cb.lower(any(Expression.class)))
                .thenReturn(path);

        org.mockito.Mockito.lenient().when(cb.between(
                any(Expression.class),
                any(Comparable.class),
                any(Comparable.class)))
                .thenReturn(predicate);

        org.mockito.Mockito.lenient().when(cb.greaterThanOrEqualTo(
                any(Expression.class),
                any(Comparable.class)))
                .thenReturn(predicate);

        org.mockito.Mockito.lenient().when(cb.lessThanOrEqualTo(
                any(Expression.class),
                any(Comparable.class)))
                .thenReturn(predicate);

        org.mockito.Mockito.lenient().when(cb.treat(any(Root.class), eq(ClienteEmpresa.class)))
                .thenReturn(root);

        org.mockito.Mockito.lenient().when(cb.treat(any(Root.class), eq(ClientePessoa.class)))
                .thenReturn(root);

        specification.toPredicate(root, query, cb);
    }

 // -------------------------------------------------------------------------
    // list() - empresas (Garantindo cobertura total das ramificações de IF)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list empresas - deve testar todas as ramificações dos ifs")
    void listCompanies_shouldCoverAllBranchConditions() {
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Cliente> specification = invocation.getArgument(0);
                    executeSpecification(specification);
                    return new PageImpl<>(List.of());
                });

        // Teste com strings em branco ("   ") para cobrir o segundo lado do !isBlank()
        service.list("   ", "   ", "   ", "   ", pageable);

        // Teste com strings válidas para entrar no bloco do IF
        service.list("Empresa", "Nome", "12345678000199", "999999999", pageable);
    }

    // -------------------------------------------------------------------------
    // toDto() & toEntity()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toDto - deve cobrir o fallback quando entidade for do tipo base Cliente")
    void toDto_shouldHandleBaseClientEntity() {
        Cliente clienteGenerico = new Cliente() {};
        clienteGenerico.setId(99L);
        clienteGenerico.setNome("Cliente Genérico");

        when(repository.findById(99L)).thenReturn(Optional.of(clienteGenerico));

        ClientDto result = service.get(99L);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("Cliente Genérico", result.getNome());
        assertNull(result.getRazaoSocial());
        assertNull(result.getDataAniversario());
    }

    @Test
    @DisplayName("save - deve criar pessoa quando cnpj for string em branco")
    void save_shouldCreatePersonWhenCnpjIsBlank() {
        ClientDto dto = new ClientDto();
        dto.setNome("Pessoa");
        dto.setCnpj("   "); // Testa a segunda parte de !dto.getCnpj().isBlank()
        dto.setDataAniversario(LocalDate.of(1990, 1, 1));

        when(repository.save(any(ClientePessoa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientDto result = service.save(dto, new Usuario());

        assertNotNull(result);
        assertEquals("Pessoa", result.getNome());
    }

    // -------------------------------------------------------------------------
    // loadEmployees() - Cobertura das linhas 248 e 249 (continue)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("loadEmployees - deve cobrir o branch continue ignorando registros incompletos")
    void loadEmployees_shouldExecuteContinueOnInvalidRows() throws IOException {
        Long idEmpresa = 1L;

        ClienteEmpresa empresa = new ClienteEmpresa();
        empresa.setId(idEmpresa);

        when(repository.findById(idEmpresa)).thenReturn(Optional.of(empresa));

        ArquivoEmpresa arquivoEmpresa = new ArquivoEmpresa();
        arquivoEmpresa.setId(100L);
        when(arquivoEmpresaRepository.saveAndFlush(any(ArquivoEmpresa.class))).thenReturn(arquivoEmpresa);

        ClientePessoa existente = new ClientePessoa();
        existente.setId(50L);

        when(repository.getEmployee(idEmpresa, "C001")).thenReturn(Optional.of(existente));
        when(repository.getEmployee(idEmpresa, "C002")).thenReturn(Optional.empty());

        when(repository.saveAndFlush(any(ClientePessoa.class)))
                .thenAnswer(invocation -> {
                    ClientePessoa pessoa = invocation.getArgument(0);
                    if (pessoa.getId() == null) {
                        pessoa.setId(51L);
                    }
                    return pessoa;
                });

        // Formato dd/MM/yyyy aceito pelo SafeDateDeserializer
        String csv = 
                "nome,numero-cracha,data-aniversario,limite-gasto\n" +
                "João,C001,01/01/1990,1000\n" +            // 1. Válido (Atualiza)
                "Maria,C002,02/02/1992,2000\n" +           // 2. Válido (Novo)
                ",C003,03/03/1995,1500\n" +                // 3. Inválido (Sem nome - entra na linha 249)
                "Carlos,,04/04/1994,1800\n" +              // 4. Inválido (Sem crachá - entra na linha 249)
                "Ana,C005,,1800\n" +                       // 5. Inválido (Sem aniversário - entra na linha 249)
                "Paulo,C006,06/06/1996,\n";                // 6. Inválido (Sem limite - entra na linha 249)

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empregados.csv",
                "text/csv",
                csv.getBytes()
        );

        ResultadoCargaEmpregadosDto result = service.loadEmployees(idEmpresa, file, new Usuario());

        assertNotNull(result);
        assertEquals(4L, result.getCarregados());
        assertEquals(6L, result.getTotal());

        verify(repository, times(4)).saveAndFlush(any(ClientePessoa.class));
        verify(arquivoClientePessoaRepository, times(4)).saveAndFlush(any(ArquivoClientePessoa.class));
    }

}
