package br.com.dinamica.estoque.dto;

import org.springframework.beans.BeanUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StockTransferProductDto extends ProductDto {

    private Integer estoqueDestino;

    public StockTransferProductDto(ProductDto product) {
    	BeanUtils.copyProperties(product, this);
    }

}
