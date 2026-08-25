package uy.com.fulbito.dto;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import uy.com.fulbito.dto.VenueDtos.PublicVenueResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class VenueDtosPrivacyTest {
    @Test
    void publicVenueNeverSerializesOwnerIdentity() throws Exception {
        PublicVenueResponse response = new PublicVenueResponse(
            UUID.randomUUID(), "Complejo", null, null, null, (short) 4, null, null
        );

        String json = JsonMapper.builder().build().writeValueAsString(response);

        assertFalse(json.contains("owner"));
        assertFalse(json.contains("email"));
        assertFalse(json.contains("nationalId"));
    }
}
