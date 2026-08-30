package br.com.dinamica.estoque.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.EmployeeDto;
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
import br.com.dinamica.estoque.service.ClientService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ClientServiceImpl implements ClientService {

	private static final String LIMITE = "limite";

	private static final String DATA_ANIVERSARIO = "dataAniversario";

	private ClienteRepository repository;

	private ContatoClienteEmpresaRepository contatoClienteEmpresaRepository;

	private ContatoClientePessoaRepository contatoClientePessoaRepository;

	private ArquivoEmpresaRepository arquivoEmpresaRepository;

	private ArquivoClientePessoaRepository arquivoClientePessoaRepository;

	private String fileSystem;

	public ClientServiceImpl(
			ClienteRepository repository,
			ContatoClienteEmpresaRepository contatoClienteEmpresaRepository,
			ContatoClientePessoaRepository contatoClientePessoaRepository,
			ArquivoEmpresaRepository arquivoEmpresaRepository,
			ArquivoClientePessoaRepository arquivoClientePessoaRepository,
			@Value("${estoque.sistema-arquivos}") String fileSystem
	) {
		this.repository = repository;
		this.contatoClienteEmpresaRepository = contatoClienteEmpresaRepository;
		this.contatoClientePessoaRepository = contatoClientePessoaRepository;
		this.arquivoEmpresaRepository = arquivoEmpresaRepository;
		this.arquivoClientePessoaRepository = arquivoClientePessoaRepository;
		this.fileSystem = fileSystem;
	}

	@Override
	public ClientDto get(Long id) {
		Cliente entity = this.repository.findById(id).orElseThrow();

		return this.toDto(entity);
	}

	@Override
	public List<CommonClientDto> findAll() {
		List<Cliente> clientes = this.repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));

		return clientes.stream().map(cliente -> new CommonClientDto(cliente.getId(), cliente.getNome())).toList();
	}

	@Override
	public Page<ClientDto> list(String razaoSocial, String nome, String cnpj, String fone, Pageable pageable) {
	    Specification<Cliente> specification = (root, _, cb) -> cb.equal(root.type(), ClienteEmpresa.class);

	    if (razaoSocial != null && !razaoSocial.isBlank()) {
	        specification = specification.and((root, _, cb) -> {
	            var empresaRoot = cb.treat(root, ClienteEmpresa.class);
	            return cb.like(cb.lower(empresaRoot.get("razaoSocial")), "%" + razaoSocial.toLowerCase() + "%");
	        });
	    }

	    if (nome != null && !nome.isBlank()) {
	        specification = specification.and((root, _, cb) -> 
	            cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%")
	        );
	    }

	    if (cnpj != null && !cnpj.isBlank()) {
	        specification = specification.and((root, _, cb) -> {
	            var empresaRoot = cb.treat(root, ClienteEmpresa.class);
	            return cb.equal(empresaRoot.get("cnpj"), cnpj);
	        });
	    }

	    if (fone != null && !fone.isBlank()) {
	        specification = specification.and((root, _, cb) -> 
	            cb.equal(root.get("fone"), fone)
	        );
	    }

	    return this.repository.findAll(specification, pageable).map(this::toDto);
	}

	@Override
	public Page<ClientDto> list(PersonFilterDto filter, Pageable pageable) {
        Specification<Cliente> specification = (root, _, cb) -> cb.equal(root.type(), ClientePessoa.class);

        if (filter.getNome() != null && !filter.getNome().isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("nome")), "%" + filter.getNome().toLowerCase() + "%"));
        }

        if (filter.getIdEmpresa() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.equal(pessoaRoot.get("empresa").get("id"), filter.getIdEmpresa());
            });
        }

        if (filter.getFone() != null && !filter.getFone().isBlank()) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("fone"), filter.getFone()));
        }

        if (filter.getMinLimite() != null && filter.getMaxLimite() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.between(pessoaRoot.get(LIMITE), filter.getMinLimite(), filter.getMaxLimite());
            });
        } else if (filter.getMinLimite() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.greaterThanOrEqualTo(pessoaRoot.get(LIMITE), filter.getMinLimite());
            });
        } else if (filter.getMaxLimite() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.lessThanOrEqualTo(pessoaRoot.get(LIMITE), filter.getMaxLimite());
            });
        }

        if (filter.getMinAniversario() != null && filter.getMaxAniversario() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.between(pessoaRoot.get(DATA_ANIVERSARIO), filter.getMinAniversario(), filter.getMaxAniversario());
            });
        } else if (filter.getMinAniversario() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.greaterThanOrEqualTo(pessoaRoot.get(DATA_ANIVERSARIO), filter.getMinAniversario());
            });
        } else if (filter.getMaxAniversario() != null) {
            specification = specification.and((root, _, cb) -> {
                var pessoaRoot = cb.treat(root, ClientePessoa.class);
                return cb.lessThanOrEqualTo(pessoaRoot.get(DATA_ANIVERSARIO), filter.getMaxAniversario());
            });
        }

		return this.repository.findAll(specification, pageable).map(this::toDto);
	}

	@Override
	public ClientDto save(ClientDto dto, Usuario usuario) {
		Cliente entity;
        LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = dto.getDataAniversario() != null ? new ClientePessoa() : new ClienteEmpresa();

			entity.setDataCriacao(agora);
		}

		this.toEntity(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.contatoClienteEmpresaRepository.deleteByCliente_Id(id);
		this.contatoClientePessoaRepository.deleteByCliente_Id(id);
		this.repository.deleteById(id);
	}

	@Override
	public ResultadoCargaEmpregadosDto loadEmployees(Long idEmpresa, MultipartFile file, Usuario usuario) throws IOException {
		Cliente empresa = this.repository.findById(idEmpresa).orElseThrow();
		ArquivoEmpresa arquivoEmpresa = new ArquivoEmpresa();
		LocalDateTime agora = DateUtil.now();

		arquivoEmpresa.setEmpresa((ClienteEmpresa) empresa);
		arquivoEmpresa.setArquivo(file.getOriginalFilename());
		arquivoEmpresa.setUsuario(usuario);
		arquivoEmpresa.setDataCriacao(agora);
		arquivoEmpresa.setDataAlteracao(agora);

		arquivoEmpresa = this.arquivoEmpresaRepository.saveAndFlush(arquivoEmpresa);

		Path diretorio = Paths.get(this.fileSystem + "/empregados/" + arquivoEmpresa.getId());
		Files.createDirectories(diretorio);
		Path arquivo = diretorio.resolve(file.getOriginalFilename());

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, arquivo, StandardCopyOption.REPLACE_EXISTING);
		}

		CsvMapper csvMapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		long carregados = 0;
		long total = 0;
		try (Reader reader = Files.newBufferedReader(arquivo)) {
            MappingIterator<EmployeeDto> it = csvMapper.readerFor(EmployeeDto.class).with(schema).readValues(reader);

            while (it.hasNext()) {
            	EmployeeDto pessoa = it.next();

            	total++;
            	if (pessoa.getNome() == null || pessoa.getCracha() == null || pessoa.getDataAniversario() == null || pessoa.getLimite() == null) {
            		continue;
            	}

            	Optional<ClientePessoa> entity = this.repository.getEmployee(idEmpresa, pessoa.getCracha());
            	ClientePessoa clientePessoa;

            	if (entity.isPresent()) {
            		clientePessoa = entity.get();
            	} else {
            		clientePessoa = new ClientePessoa();
            	}

            	clientePessoa.setNome(pessoa.getNome());
            	clientePessoa.setEmpresa(empresa);
            	clientePessoa.setCracha(pessoa.getCracha());
            	clientePessoa.setDataAniversario(pessoa.getDataAniversario());
            	clientePessoa.setLimite(new BigDecimal(pessoa.getLimite()));
            	clientePessoa.setUsuario(usuario);
            	if (clientePessoa.getId() == null) {
            		clientePessoa.setDataCriacao(agora);
            	}
            	clientePessoa.setDataAlteracao(agora);

            	clientePessoa = this.repository.saveAndFlush(clientePessoa);

            	ArquivoClientePessoa arquivoClientePessoa = new ArquivoClientePessoa();

            	arquivoClientePessoa.setArquivoEmpresa(arquivoEmpresa);
            	arquivoClientePessoa.setPessoaCliente(clientePessoa);
            	arquivoClientePessoa.setUsuario(usuario);
            	arquivoClientePessoa.setDataCriacao(agora);
            	arquivoClientePessoa.setDataAlteracao(agora);

            	this.arquivoClientePessoaRepository.saveAndFlush(arquivoClientePessoa);
            	carregados++;
            }
        }

		return new ResultadoCargaEmpregadosDto(carregados, total);
	}

	private ClientDto toDto(Cliente entity) {
		Cliente unproxiedEntity = (Cliente) Hibernate.unproxy(entity);
		ClientDto result = new ClientDto();

		result.setId(unproxiedEntity.getId());
		result.setNome(unproxiedEntity.getNome());
		result.setFone(unproxiedEntity.getFone());
		result.setEndereco(unproxiedEntity.getEndereco());
		result.setBairro(unproxiedEntity.getBairro());
		result.setCep(unproxiedEntity.getCep());
		result.setCidade(unproxiedEntity.getCidade());
		result.setUf(unproxiedEntity.getUf());

		if (unproxiedEntity instanceof ClientePessoa pessoa) {
			if (pessoa.getEmpresa() != null) {
				result.setEmpresa((this.toDto(pessoa.getEmpresa())));
			}

			result.setDataAniversario(pessoa.getDataAniversario());
			result.setLimite(pessoa.getLimite());
			result.setCracha(pessoa.getCracha());
		} else if (unproxiedEntity instanceof ClienteEmpresa empresa) {
			result.setRazaoSocial(empresa.getRazaoSocial());
			result.setCnpj(empresa.getCnpj());
		}

		return result;
	}

	private void toEntity(ClientDto dto, Cliente entity) {
	    if (dto.getCnpj() != null && !dto.getCnpj().isBlank()) {
	        ClienteEmpresa empresa = (ClienteEmpresa) entity;

	        empresa.setRazaoSocial(dto.getRazaoSocial());
	        empresa.setCnpj(dto.getCnpj());
	    } else {
	        ClientePessoa pessoa = (ClientePessoa) entity;

			if (dto.getEmpresa() != null) {
				Cliente empresa = this.repository.findById(dto.getEmpresa().getId()).orElseThrow();

				((ClientePessoa) entity).setEmpresa(empresa);
			}

			pessoa.setDataAniversario(dto.getDataAniversario());
	        pessoa.setLimite(dto.getLimite());
	        pessoa.setCracha(dto.getCracha());
	    }

	    entity.setId(dto.getId());
	    entity.setNome(dto.getNome());
	    entity.setFone(dto.getFone());
	    entity.setEndereco(dto.getEndereco());
	    entity.setBairro(dto.getBairro());
	    entity.setCep(dto.getCep());
	    entity.setCidade(dto.getCidade());
	    entity.setUf(dto.getUf());
	}

}
