package br.com.dinamica.estoque.entity;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido", nullable = false, length = 100)
    private String numeroPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fornecedor", nullable = false, foreignKey = @ForeignKey(name = "fk_pedido_fornecedor"))
    private Fornecedor fornecedor;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_pedido", nullable = false)
    private Date dataPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_pedido_compra_usuario"))
    private Usuario usuario;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

}