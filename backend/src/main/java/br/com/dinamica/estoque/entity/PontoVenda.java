package br.com.dinamica.estoque.entity;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ponto_venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PontoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_ponto_venda_usuario"))
    private Usuario usuario;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_alteracao", nullable = false)
    private Date dataAlteracao;

}