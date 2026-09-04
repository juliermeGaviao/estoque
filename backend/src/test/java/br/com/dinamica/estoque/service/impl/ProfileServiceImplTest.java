package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.dinamica.estoque.dto.ProfileDto;
import br.com.dinamica.estoque.entity.Perfil;
import br.com.dinamica.estoque.mapper.ProfileMapper;
import br.com.dinamica.estoque.repository.PerfilRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProfileMapper modelMapper;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(perfilRepository, modelMapper);
    }

    @Test
    @DisplayName("getProfiles - deve retornar lista convertida de ProfileDto quando existirem perfis")
    void getProfiles_shouldReturnMappedDtoList() {
        Perfil perfil1 = new Perfil();
        perfil1.setId(1L);

        Perfil perfil2 = new Perfil();
        perfil2.setId(2L);

        ProfileDto dto1 = new ProfileDto();
        ProfileDto dto2 = new ProfileDto();

        when(perfilRepository.findAll()).thenReturn(List.of(perfil1, perfil2));
        when(modelMapper.toDto(perfil1)).thenReturn(dto1);
        when(modelMapper.toDto(perfil2)).thenReturn(dto2);

        List<ProfileDto> result = service.getProfiles();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(dto1, result.get(0));
        assertEquals(dto2, result.get(1));

        verify(perfilRepository).findAll();
        verify(modelMapper).toDto(perfil1);
        verify(modelMapper).toDto(perfil2);
    }

    @Test
    @DisplayName("getProfiles - deve retornar lista vazia quando nao houver perfis cadastrados")
    void getProfiles_shouldReturnEmptyListWhenNoProfilesExist() {
        when(perfilRepository.findAll()).thenReturn(List.of());

        List<ProfileDto> result = service.getProfiles();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(perfilRepository).findAll();
    }
}