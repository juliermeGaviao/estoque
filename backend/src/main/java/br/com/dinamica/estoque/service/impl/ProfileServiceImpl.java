package br.com.dinamica.estoque.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProfileDto;
import br.com.dinamica.estoque.entity.Perfil;
import br.com.dinamica.estoque.repository.PerfilRepository;
import br.com.dinamica.estoque.service.ProfileService;

@Service
public class ProfileServiceImpl implements ProfileService {

	private PerfilRepository perfilRepository;

	private ModelMapper modelMapper;

	public ProfileServiceImpl(PerfilRepository perfilRepository, ModelMapper modelMapper) {
		this.perfilRepository = perfilRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public List<ProfileDto> getProfiles() {
		List<ProfileDto> result = new ArrayList<>();

		for (Perfil perfil: this.perfilRepository.findAll()) {
			result.add(this.modelMapper.map(perfil, ProfileDto.class));
		}

		return result;
	}

}
