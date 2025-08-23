package br.com.dinamica.estoque.dto;

import java.util.Arrays;

public record AuthResponse(Long id, String token, String[] perfis) {

	@Override
	public boolean equals(Object o) {
	    return this == o ||
	    		(o instanceof AuthResponse(Long otherId, String otherToken, String[] otherPerfis)
	                && java.util.Objects.equals(id, otherId)
	                && java.util.Objects.equals(token, otherToken)
	                && Arrays.equals(perfis, otherPerfis));
	}

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, token, Arrays.hashCode(perfis));
    }

    @Override
    public String toString() {
        return "AuthResponse[id=" + id + ", token=" + token + ", perfis=" + Arrays.toString(perfis) + ']';
    }

}
