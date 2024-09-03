package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import com.intela.realestatebackend.models.property.Property;
import com.intela.realestatebackend.models.property.PropertyImage;
import com.intela.realestatebackend.requestResponse.PropertyCreationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.sql.Timestamp;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PropertyControllerTest extends BaseTestContainerTest {

    @Autowired
    private ObjectMapper objectMapper;

    // Sample property data
    private static final String PROPERTY_OWNER_NAME = "Mock User";
    private static final String PROPERTY_LOCATION = "Downtown";
    private static final String PROPERTY_DESCRIPTION = "A luxury apartment in downtown.";
    private static final int PROPERTY_ROOMS = 3;
    private static final PropertyType PROPERTY_TYPE = PropertyType.APARTMENT;
    private static final String PROPERTY_STATUS = "Available";
    private static final long PROPERTY_PRICE = 1200000L;
    // Have not set up the bill type
    //private static final BillType PROPERTY_BILL_TYPE = BillType.INCLUSIVE;
    private static final Timestamp PROPERTY_AVAILABLE_FROM = Timestamp.valueOf("2024-09-01 00:00:00");
    private static final Timestamp PROPERTY_AVAILABLE_TILL = Timestamp.valueOf("2024-12-31 23:59:59");
    private static final List<String> PROPERTY_IMAGES = List.of("image_url_1", "image_url_2");

    // Sample updated property data
    private static final String UPDATED_PROPERTY_OWNER_NAME = "Jane Doe";
    private static final String UPDATED_PROPERTY_DESCRIPTION = "Updated luxury apartment in downtown.";
    private static final List<String> UPDATED_PROPERTY_IMAGES = List.of("updated_image_url_1", "updated_image_url_2");

    /*
    End point needs to be defined
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldCreateProperty() throws Exception {
        PropertyCreationRequest request = new PropertyCreationRequest();
        request.setPropertyOwnerName(PROPERTY_OWNER_NAME);
        request.setLocation(PROPERTY_LOCATION);
        request.setDescription(PROPERTY_DESCRIPTION);
        request.setNumberOfRooms(PROPERTY_ROOMS);
        request.setPropertyType(PROPERTY_TYPE);
        request.setStatus(PROPERTY_STATUS);
        request.setPrice(PROPERTY_PRICE);
        //request.setBillType(PROPERTY_BILL_TYPE);
        request.setAvailableFrom(PROPERTY_AVAILABLE_FROM);
        request.setAvailableTill(PROPERTY_AVAILABLE_TILL);
        //request.setImages(PROPERTY_IMAGES);

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetPropertyById() throws Exception {
        int propertyId = 1;  // Example property ID

        mockMvc.perform(get("/api/v1/properties/" + propertyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(propertyId))
                .andExpect(jsonPath("$.propertyOwnerName").value(PROPERTY_OWNER_NAME))
                .andExpect(jsonPath("$.description").value(PROPERTY_DESCRIPTION))
                .andExpect(jsonPath("$.location").value(PROPERTY_LOCATION))
                .andExpect(jsonPath("$.numberOfRooms").value(PROPERTY_ROOMS))
                .andExpect(jsonPath("$.propertyType").value(PROPERTY_TYPE.toString()))
                .andExpect(jsonPath("$.status").value(PROPERTY_STATUS))
                .andExpect(jsonPath("$.price").value(PROPERTY_PRICE))
                //.andExpect(jsonPath("$.billType").value(PROPERTY_BILL_TYPE.toString()))
                .andExpect(jsonPath("$.availableFrom").value(PROPERTY_AVAILABLE_FROM.getTime()))
                .andExpect(jsonPath("$.availableTill").value(PROPERTY_AVAILABLE_TILL.getTime()))
                .andExpect(jsonPath("$.images").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetPropertyImagesById() throws Exception {
        int propertyId = 1;  // Example property ID

        mockMvc.perform(get("/api/v1/properties/images/" + propertyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(PROPERTY_IMAGES.get(0)));  // Example check for the first image URL
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldDeletePropertyById() throws Exception {
        int propertyId = 1;  // Example property ID

        mockMvc.perform(delete("/api/v1/properties/" + propertyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Property has been deleted successfully"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdatePropertyById() throws Exception {
        int propertyId = 1;  // Example property ID

        String updatePropertyJson = "{ \"propertyOwnerName\": \"" + UPDATED_PROPERTY_OWNER_NAME + "\", \"description\": \"" + UPDATED_PROPERTY_DESCRIPTION + "\", \"images\": " + objectMapper.writeValueAsString(UPDATED_PROPERTY_IMAGES) + " }";

        mockMvc.perform(put("/api/v1/properties/" + propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePropertyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyOwnerName").value(UPDATED_PROPERTY_OWNER_NAME))
                .andExpect(jsonPath("$.description").value(UPDATED_PROPERTY_DESCRIPTION))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images[0]").value("updated_image_url_1"));
    }

}
