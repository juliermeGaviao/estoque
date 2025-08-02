package br.com.dinamica.estoque.configuration;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

	@Bean
	ModelMapper modelMapperBean() {
		ModelMapper modelMapper = new ModelMapper();

		// Não mapear campos @OneToMany
		modelMapper.getConfiguration().setPropertyCondition(context -> !(context.getSource() instanceof List));

		return modelMapper;
	}

}
