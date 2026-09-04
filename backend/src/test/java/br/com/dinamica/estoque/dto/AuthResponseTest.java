package br.com.dinamica.estoque.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthResponseTest {

    @Test
    @DisplayName("Deve testar criação do Record, getters, equals, hashCode e toString")
    void testAuthResponseRecordMethods() {
        Long id = 1L;
        String token = "jwt.token.here";
        String[] perfis = new String[]{"ROLE_ADMIN", "ROLE_USER"};

        AuthResponse authResponse1 = new AuthResponse(id, token, perfis);
        AuthResponse authResponse2 = new AuthResponse(id, token, new String[]{"ROLE_ADMIN", "ROLE_USER"});
        AuthResponse authResponseDiffId = new AuthResponse(2L, token, perfis);
        AuthResponse authResponseDiffToken = new AuthResponse(id, "diff.token", perfis);
        AuthResponse authResponseDiffPerfis = new AuthResponse(id, token, new String[]{"ROLE_GUEST"});

        // Accessors do Record
        assertEquals(id, authResponse1.id());
        assertEquals(token, authResponse1.token());
        assertEquals(perfis, authResponse1.perfis());

        // Equals (mesmo objeto, objetos equivalentes e divergentes)
        assertEquals(authResponse1, authResponse1);
        assertEquals(authResponse1, authResponse2);
        assertNotEquals(authResponse1, authResponseDiffId);
        assertNotEquals(authResponse1, authResponseDiffToken);
        assertNotEquals(authResponse1, authResponseDiffPerfis);
        assertNotEquals(authResponse1, null);
        assertNotEquals(authResponse1, "outro_objeto");

        // HashCode
        assertEquals(authResponse1.hashCode(), authResponse2.hashCode());
        assertNotEquals(authResponse1.hashCode(), authResponseDiffId.hashCode());

        // ToString
        String toStringResult = authResponse1.toString();
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("id=1"));
        assertTrue(toStringResult.contains("token=jwt.token.here"));
    }
}