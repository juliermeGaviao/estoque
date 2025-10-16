package br.com.dinamica.estoque.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
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

    @Column(name = "data_aniversario", nullable = false, columnDefinition = "date")
    private Date dataAniversario;

}
