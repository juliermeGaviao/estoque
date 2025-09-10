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
@Table(name = "contato_cliente_empresa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContatoClienteEmpresa {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_empresa", nullable = false)
    private ClienteEmpresa clienteEmpresa;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "cargo", nullable = false, length = 255)
    private String cargo;

    @Column(name = "fone", nullable = false, length = 11, columnDefinition = "char")
    private String fone;

    @Column(name = "ramal", nullable = false, length = 20)
    private String ramal;

    @Column(name = "celular", nullable = false, length = 11, columnDefinition = "char")
    private String celular;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "data_aniversario", columnDefinition = "date")
    private Date dataAniversario;

    @Column(name = "observacoes", nullable = false, length = 1024)
    private String observacoes;

    @Column(name = "data_criacao", nullable = false)
    private Date dataCriacao;

    @Column(name = "data_alteracao", nullable = false)
    private Date dataAlteracao;

}
