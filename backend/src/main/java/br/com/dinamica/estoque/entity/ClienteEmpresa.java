package br.com.dinamica.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cliente_empresa")
@DiscriminatorValue("J")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClienteEmpresa extends Cliente {

    @Column(name = "razao_social", nullable = false, length = 255)
    private String razaoSocial;

    @Column(name = "cnpj", nullable = false, length = 14, columnDefinition = "char", unique = true)
    private String cnpj;

}
