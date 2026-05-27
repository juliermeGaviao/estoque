package br.com.dinamica.estoque.entity;

import java.math.BigDecimal;
import java.util.Date;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venda {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tabela_preco", nullable = false)
    private TabelaPreco tabela;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ponto_venda", nullable = false, foreignKey = @ForeignKey(name = "fk_venda_ponto_venda"))
    private PontoVenda pontoVenda;

    @Column(name = "sub_total", nullable = false, precision = 19, scale = 2, columnDefinition = "decimal")
    private BigDecimal subTotal;

    @Column(name = "desconto")
    private Float desconto;

    @Column(name = "total", nullable = false, precision = 19, scale = 2, columnDefinition = "decimal")
    private BigDecimal total;

    @Column(name = "observacoes", length = 1024)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Date dataCriacao;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_alteracao", nullable = false)
    private Date dataAlteracao;

}
