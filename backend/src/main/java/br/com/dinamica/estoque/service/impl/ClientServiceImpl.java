package br.com.dinamica.estoque.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
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

	private ModelMapper modelMapper;

	private String fileSystem;

	public ClientServiceImpl(
			ClienteRepository repository,
			ContatoClienteEmpresaRepository contatoClienteEmpresaRepository,
			ContatoClientePessoaRepository contatoClientePessoaRepository,
			ArquivoEmpresaRepository arquivoEmpresaRepository,
			ArquivoClientePessoaRepository arquivoClientePessoaRepository,
			ModelMapper modelMapper,
			@Value("${estoque.sistema-arquivos}") String fileSystem
	) {
		this.repository = repository;
		this.contatoClienteEmpresaRepository = contatoClienteEmpresaRepository;
		this.contatoClientePessoaRepository = contatoClientePessoaRepository;
		this.arquivoEmpresaRepository = arquivoEmpresaRepository;
		this.arquivoClientePessoaRepository = arquivoClientePessoaRepository;
		this.modelMapper = modelMapper;
		this.fileSystem = fileSystem;

		this.modelMapper.addMappings(new PropertyMap<ClientDto, ClientePessoa>() {
            @Override
            protected void configure() {
                skip(destination.getEmpresa());
            }
        });
	}

	@Override
	public ClientDto get(Long id) {
		Cliente entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ClientDto.class);
	}

	@Override
	public List<CommonClientDto> findAll() {
		List<Cliente> clientes = this.repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));

		return clientes.stream().map(cliente -> new CommonClientDto(cliente.getId(), cliente.getNome())).toList();
	}

	@Override
	public Page<ClientDto> list(String razaoSocial, String nome, String cnpj, String fone, Pageable pageable) {
        Specification<Cliente> specification = (root, query, cb) -> cb.equal(root.type(), ClienteEmpresa.class);

        if (razaoSocial != null && !razaoSocial.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("razaoSocial")), "%" + razaoSocial.toLowerCase() + "%"));
        }

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

        if (cnpj != null && !cnpj.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("cnpj"), cnpj));
        }

        if (fone != null && !fone.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fone"), fone));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ClientDto.class));
	}

	@Override
	public Page<ClientDto> list(String nome, Long idEmpresa, String fone, BigDecimal minLimite, BigDecimal maxLimite, Date minAniversario, Date maxAniversario, Pageable pageable) {
        Specification<Cliente> specification = (root, query, cb) -> cb.equal(root.type(), ClientePessoa.class);

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

        if (idEmpresa != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("empresa").get("id"), idEmpresa));
        }

        if (fone != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fone"), fone));
        }

        if (minLimite != null && maxLimite != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(LIMITE), minLimite, maxLimite));
        } else if (minLimite != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(LIMITE), minLimite));
        } else if (maxLimite != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(LIMITE), maxLimite));
        }

        if (minAniversario != null && maxAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(DATA_ANIVERSARIO), minAniversario, maxAniversario));
        } else if (minAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DATA_ANIVERSARIO), minAniversario));
        } else if (maxAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(DATA_ANIVERSARIO), maxAniversario));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ClientDto.class));
	}

	@Override
	public ClientDto save(ClientDto dto, Usuario usuario) {
		Cliente entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = dto.getDataAniversario() != null ? new ClientePessoa() : new ClienteEmpresa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		if (dto.getEmpresa() != null) {
			Cliente empresa = this.repository.findById(dto.getEmpresa().getId()).orElseThrow();

			((ClientePessoa) entity).setEmpresa(empresa);
		}

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ClientDto.class);
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
        Date agora = DateUtil.now();

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

}
