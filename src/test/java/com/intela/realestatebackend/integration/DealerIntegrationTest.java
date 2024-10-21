package com.intela.realestatebackend.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PaymentCycle;
import com.intela.realestatebackend.models.archetypes.PropertyStatus;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import com.intela.realestatebackend.models.property.Feature;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.intela.realestatebackend.testUtil.TestUtil.IMAGES_PATH;
import static com.intela.realestatebackend.testUtil.TestUtil.cleanDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DealerIntegrationTest extends BaseTestContainerTest {
    private static List<TestUser> dealerUsers;
    @Autowired
    private List<TestUser> allUsers;

    @AfterAll
    public static void cleanUp() throws IOException {
        // Delete all files in the ./resources/images directory
        cleanDirectory(IMAGES_PATH);
    }

    @Test
    @Order(1)
    void shouldRegisterUser() throws Exception {
        dealerUsers = TestUtil.testRegisterDealerUsers(mockMvc, objectMapper, allUsers);
    }

    @Test
    @Order(2)
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
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
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
        s = mockMvc.perform(get("/api/v1/dealer/property/images/{propertyId}", propertyId)
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
        s = mockMvc.perform(get("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        PropertyResponse propertyResponse = objectMapper.readValue(s, PropertyResponse.class);
        Assertions.assertEquals(propertyResponse.getUser(), propertyRequest.getUser());
        propertyResponse.getFeature().setId(null);
        Assertions.assertEquals(propertyResponse.getFeature(), propertyRequest.getFeature());
        Assertions.assertEquals(propertyResponse.getNumberOfRooms(), propertyRequest.getNumberOfRooms());
        Assertions.assertEquals(propertyResponse.getNumberOfRooms(),
                propertyRequest.getFeature().getBedrooms() + propertyRequest.getFeature().getLounges());
        Assertions.assertEquals(propertyResponse.getStatus(), PropertyStatus.AVAILABLE);
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(3)
    public void testUploadPropertyInvalidRequest() throws Exception {
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Create PropertyRequest object with missing required fields
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();

        // Step 3: Convert PropertyRequest object to JSON string
        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Step 4: Mock image file
        MockMultipartFile imageFile1 = new MockMultipartFile("images", "image.png", "image/png", image1Bytes);
        MockMultipartFile imageFile2 = new MockMultipartFile("images", "image.png", "image/png", image2Bytes);

        // Step 5: Perform the test with Authorization header
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();
        propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();
        propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .billType(BillType.INCLUDED)
                .price(650L)
                .build();
        propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(4)
    public void testUploadPropertyInvalidFileType() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Create PropertyRequest object
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();

        // Step 3: Convert PropertyRequest object to JSON string
        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Step 4: Mock an unsupported file type (text file)
        MockMultipartFile invalidFile = new MockMultipartFile("images", "file.txt", "text/plain", "invalid file data".getBytes());

        // Step 5: Perform the test with Authorization header
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(5)
    public void testUploadPropertyLargeFile() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Create PropertyRequest object
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();

        // Step 3: Convert PropertyRequest object to JSON string
        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Step 4: Simulate a large file (over 10MB)
        byte[] largeFileData = new byte[11 * 1024 * 1024];
        MockMultipartFile largeImageFile = new MockMultipartFile("images", "large_image.png", "image/png", largeFileData);

        // Step 5: Perform the test with Authorization header
        mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(largeImageFile)
                        .header("Authorization", "Bearer " + accessToken)  // Attach access token
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isPayloadTooLarge());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(6)
    public void testGetPropertyById_NonExistentId_ReturnsNotFound() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Request for a non-existent property ID (e.g., ID = 9999)
        int propertyId = -2;

        // Step 3: Perform the test
        mockMvc.perform(get("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(7)
    public void testGetPropertyById_InvalidIdFormat_ReturnsBadRequest() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Use an invalid ID format (e.g., "invalid-id")
        String propertyId = "invalid-id";

        // Step 3: Perform the test
        mockMvc.perform(get("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(8)
    public void testGetAllProperties_UpdateFirstAndVerify() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Get all properties
        MvcResult result = mockMvc.perform(get("/api/v1/dealer/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response into a list of PropertyResponse objects
        String content = result.getResponse().getContentAsString();
        List<PropertyResponse> properties = objectMapper.readValue(content, new TypeReference<>() {
        });

        // Ensure that there are properties returned
        Assertions.assertFalse(properties.isEmpty());

        // Step 3: Select the first property from the list
        PropertyResponse firstProperty = properties.get(0);
        Integer propertyId = firstProperty.getId();

        // Step 4: Update the first property (e.g., update description)
        PropertyRequest updatedProperty = new PropertyRequest();
        BeanUtils.copyProperties(firstProperty, updatedProperty);
        updatedProperty.setDescription("Updated description");

        MockMultipartFile multipartFile = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(updatedProperty)
        );

        // Step 5: Perform the update using PUT or PATCH (depending on your API implementation)
        mockMvc.perform(multipart("/api/v1/dealer/property/{propertyId}", propertyId)
                        .file(multipartFile)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("PUT");  // Override POST to PUT
                            return request;
                        }))
                .andExpect(status().isOk());

        // Step 6: Retrieve the updated property
        MvcResult updatedResult = mockMvc.perform(get("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response into a PropertyResponse object
        PropertyResponse updatedPropertyResponse = objectMapper.readValue(updatedResult.getResponse().getContentAsString(), PropertyResponse.class);

        // Step 7: Verify the update was successful by checking the description field
        Assertions.assertEquals("Updated description", updatedPropertyResponse.getDescription());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(9)
    public void testInvalidUpdateData_FeatureNull_ReturnsBadRequest() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Get all properties
        MvcResult result = mockMvc.perform(get("/api/v1/dealer/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response into a list of PropertyResponse objects
        String content = result.getResponse().getContentAsString();
        List<PropertyResponse> properties = objectMapper.readValue(content, new TypeReference<>() {
        });

        // Ensure that there are properties returned
        Assertions.assertFalse(properties.isEmpty());

        // Step 3: Select the first property from the list
        PropertyResponse firstProperty = properties.get(0);
        Integer propertyId = firstProperty.getId();

        // Step 4: Update the first property (e.g., update description)
        PropertyRequest updatedProperty = new PropertyRequest();
        BeanUtils.copyProperties(firstProperty, updatedProperty);
        updatedProperty.setFeature(null);

        // Convert the updated property to JSON
        String updatedPropertyJson = objectMapper.writeValueAsString(updatedProperty);

        // Step 5: Perform the update using PUT or PATCH (depending on your API implementation)
        mockMvc.perform(put("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedPropertyJson))
                .andExpect(status().isBadRequest());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(10)
    public void testUpdateNonExistentProperty_ReturnsNotFound() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Define a non-existent property ID (e.g., 9999)
        int nonExistentPropertyId = -2;

        // Step 3: Create a PropertyRequest with valid data (this doesn't matter as the property doesn't exist)
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest updatedProperty = PropertyRequest.builder()
                .propertyOwnerName("John Doe")
                .feature(feature)
                .location("123 Main St, Sydney")
                .description("A beautiful 3-bedroom house")
                .propertyType(PropertyType.HOUSE)
                .price(650L)
                .paymentCycle(PaymentCycle.WEEKLY)
                .billType(BillType.INCLUDED)
                .build();
        // Convert the updated property to JSON
        String updatedPropertyJson = objectMapper.writeValueAsString(updatedProperty);

        // Step 4: Perform the update on a non-existent property ID and expect a 404 Not Found
        mockMvc.perform(put("/api/v1/dealer/" + nonExistentPropertyId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedPropertyJson))
                .andExpect(status().isNotFound());  // Adjust the error message based on your API's response format
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(11)
    public void testDeleteProperty_Success_ReturnsNoContent() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Get all properties
        MvcResult result = mockMvc.perform(get("/api/v1/dealer/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response into a list of PropertyResponse objects
        String content = result.getResponse().getContentAsString();
        List<PropertyResponse> properties = objectMapper.readValue(content, new TypeReference<>() {
        });

        // Ensure that there are properties returned
        Assertions.assertFalse(properties.isEmpty());

        // Step 3: Select the first property from the list
        PropertyResponse firstProperty = properties.get(0);
        Integer propertyId = firstProperty.getId();

        // Step 3: Perform the delete request
        mockMvc.perform(delete("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        TestUtil.testLogout(mockMvc, accessToken);
    }

    @Test
    @Order(12)
    public void testDeleteProperty_NonExistentId_ReturnsNotFound() throws Exception {
        // Step 1: Perform login and retrieve access token
        AuthenticationResponse authenticationResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String accessToken = authenticationResponse.getAccessToken();

        // Step 2: Use a non-existent property ID
        int propertyId = -2;

        // Step 3: Perform the delete request
        mockMvc.perform(delete("/api/v1/dealer/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        TestUtil.testLogout(mockMvc, accessToken);
    }

}
