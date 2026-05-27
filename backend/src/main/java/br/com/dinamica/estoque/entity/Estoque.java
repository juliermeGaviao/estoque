package br.com.dinamica.estoque.entity;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produto", nullable = false, foreignKey = @ForeignKey(name = "fk_estoque_produto"))
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ponto_venda", nullable = false, foreignKey = @ForeignKey(name = "fk_estoque_pv"))
    private PontoVenda pontoVenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido_compra", foreignKey = @ForeignKey(name = "fk_estoque_pedido"))
    private PedidoCompra pedidoCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venda", foreignKey = @ForeignKey(name = "fk_estoque_venda"))
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transferencia_estoque", foreignKey = @ForeignKey(name = "fk_estoque_transferencia"))
    private TransferenciaEstoque transferenciaEstoque;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, columnDefinition = "ENUM('C', 'D')")
    private TipoOperacao tipoOperacao;

    @Column(name = "saldo", nullable = false)
    private Integer saldo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_estoque_usuario"))
    private Usuario usuario;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

}