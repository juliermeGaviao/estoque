package br.com.dinamica.estoque.entity;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario_ponto_venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPontoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_pv_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ponto_venda", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_pv_ponto_venda"))
    private PontoVenda pontoVenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cadastro", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_pv_responsavel"))
    private Usuario usuarioCadastro;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

}
