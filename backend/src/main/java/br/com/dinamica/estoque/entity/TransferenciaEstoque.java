package br.com.dinamica.estoque.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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

    @Column(name = "data_transferencia", nullable = false)
    private LocalDateTime dataTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_transferencia_estoque_usuario"))
    private Usuario usuario;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}