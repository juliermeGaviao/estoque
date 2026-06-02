package br.com.dinamica.estoque.entity;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transferencia_estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ponto_venda_origem", nullable = false, foreignKey = @ForeignKey(name = "fk_transf_pv_origem"))
    private PontoVenda pontoVendaOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ponto_venda_destino", nullable = false, foreignKey = @ForeignKey(name = "fk_transf_pv_destino"))
    private PontoVenda pontoVendaDestino;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_transferencia", nullable = false)
    private Date dataTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_transferencia_estoque_usuario"))
    private Usuario usuario;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

}