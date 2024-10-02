package com.intela.realestatebackend.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DealerIntegrationTest extends BaseTestContainerTest {
    @Autowired
    private List<TestUser> allUsers;
    private static List<TestUser> dealerUsers;

    @Test
    @Order(1)
    void shouldRegisterUser() throws Exception {
        dealerUsers = TestUtil.testRegisterDealerUsers(mockMvc, objectMapper, allUsers);
    }

    @Test
    public void testUploadPropertySuccess() throws Exception {
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();
        String s = mockMvc.perform(
                        get("/api/v1/user/")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()
                ).andReturn().getResponse().getContentAsString();
        RetrieveAccountResponse user = objectMapper.readValue(s, RetrieveAccountResponse.class);

        // Step 2: Create PropertyRequest object
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .numberOfRooms(3)
                .propertyType(PropertyType.HOUSE)
                .price(500000L)
                .billType(BillType.INCLUDED)
                .build();

        // Step 3: Convert PropertyRequest object to JSON string
        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Step 4: Mock image file
        MockMultipartFile imageFile1 = new MockMultipartFile("images", "image1.png", "image/png", image1Bytes);
        MockMultipartFile imageFile2 = new MockMultipartFile("images", "image2.png", "image/png", image2Bytes);

        // Step 5: Perform the test with Authorization header
        s = mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        PropertyCreationResponse propertyCreationResponse = objectMapper.readValue(s, PropertyCreationResponse.class);
        Integer propertyId = propertyCreationResponse.getId();
        Assertions.assertEquals(user.getId(), propertyCreationResponse.getPropertyOwnerId());
        s = mockMvc.perform(get("/property/images/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<PropertyImageResponse> propertyImageResponses = objectMapper.readValue(s, new TypeReference<>() {
        });
        propertyImageResponses.forEach(property -> {
            if (property.getName().equals("image1.jpg")) {
                assertThat(property.getImage().length).isGreaterThan(0);
                assertThat(property.getImage()).isEqualTo(image1Bytes);
            }
            if (property.getName().equals("image2.jpg")) {
                assertThat(property.getImage().length).isGreaterThan(0);
                assertThat(property.getImage()).isEqualTo(image2Bytes);
            }
        });
    }
}
