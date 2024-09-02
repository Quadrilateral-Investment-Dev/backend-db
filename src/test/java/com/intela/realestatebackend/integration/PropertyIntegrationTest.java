package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.archetypes.Role;
import com.intela.realestatebackend.requestResponse.AuthenticationResponse;
import com.intela.realestatebackend.requestResponse.PropertyResponse;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class PropertyIntegrationTest extends BaseTestContainerTest {

    private static final String PROPERTY_NAME = "Seaside Villa";
    private static final String PROPERTY_ADDRESS = "123 Beach Road, Sydney, NSW";
    private static final String PROPERTY_DESCRIPTION = "A beautiful seaside villa with ocean views.";
    private static final int PROPERTY_PRICE = 1500000;
    private static final String PROPERTY_OWNER_EMAIL = "owner@gmail.com";
    private static final String PROPERTY_TYPE = "Villa";



    @Test
    void shouldCreateProperty() throws Exception {
        // Step 1: Prepare a property object with all required fields
        PropertyResponse propertyRequest = new PropertyResponse();
        propertyRequest.setName(PROPERTY_NAME);
        propertyRequest.setAddress(PROPERTY_ADDRESS);
        propertyRequest.setDescription(PROPERTY_DESCRIPTION);
        propertyRequest.setPrice(PROPERTY_PRICE);
        propertyRequest.setOwnerEmail(PROPERTY_OWNER_EMAIL);
        propertyRequest.setType(PROPERTY_TYPE);

        // Step 2: Perform POST /properties to create the property
        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propertyRequest)))
                .andExpect(status().isCreated())  // Assuming successful property creation returns HTTP 201
                .andExpect(jsonPath("$.name").value(PROPERTY_NAME))
                .andExpect(jsonPath("$.address").value(PROPERTY_ADDRESS))
                .andExpect(jsonPath("$.description").value(PROPERTY_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(PROPERTY_PRICE))
                .andExpect(jsonPath("$.ownerEmail").value(PROPERTY_OWNER_EMAIL))
                .andExpect(jsonPath("$.type").value(PROPERTY_TYPE));
    }
}
