package br.com.dinamica.estoque.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "arquivo_cliente_pessoa",
	uniqueConstraints = {
        @UniqueConstraint(
        	name = "uk_arquivo_empresa_cliente_pessoa",
        	columnNames = {"id_arquivo_empresa", "id_cliente_pessoa"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArquivoClientePessoa {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_arquivo_empresa", nullable = false)
    private ArquivoEmpresa arquivoEmpresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_pessoa", nullable = false)
    private ClientePessoa pessoaCliente;

    @Column(name = "data_criacao", nullable = false)
    private Date dataCriacao;

    @Column(name = "data_alteracao", nullable = false)
    private Date dataAlteracao;

}
