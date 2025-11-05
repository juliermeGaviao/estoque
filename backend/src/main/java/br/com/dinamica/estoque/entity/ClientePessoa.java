package br.com.dinamica.estoque.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cliente_pessoa")
@DiscriminatorValue("F")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientePessoa extends Cliente {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Cliente empresa;

    @Column(name = "data_aniversario", nullable = false, columnDefinition = "date")
    private Date dataAniversario;

    @Column(name = "limite", precision = 19, scale = 2, columnDefinition = "decimal")
    private BigDecimal limite;

    @Column(name = "cracha", length = 50)
    private String cracha;

}
