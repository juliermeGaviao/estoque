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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cliente_pessoa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientePessoa {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "fone", length = 11, columnDefinition = "char")
    private String fone;

    @Column(name = "data_aniversario", columnDefinition = "date")
    private Date dataAniversario;

    @Column(name = "endereco", length = 255)
    private String endereco;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cep", length = 8, columnDefinition = "char")
    private String cep;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2, columnDefinition = "char")
    private String uf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "data_criacao", nullable = false)
    private Date dataCriacao;

    @Column(name = "data_alteracao", nullable = false)
    private Date dataAlteracao;

}
