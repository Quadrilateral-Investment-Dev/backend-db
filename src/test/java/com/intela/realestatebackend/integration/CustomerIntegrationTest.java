package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.profile.ContactDetails;
import com.intela.realestatebackend.models.profile.PersonalDetails;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CustomerIntegrationTest extends BaseTestContainerTest {
    private static List<TestUser> customerUsers;
    @Autowired
    private List<TestUser> allUsers;

    @Test
    @Order(1)
    void shouldRegisterUser() throws Exception {
        customerUsers = TestUtil.testRegisterCustomerUsers(mockMvc, objectMapper, allUsers);
    }

    @Test
    @Order(1)
    public void fetchAllProperties_withDefaultPagination_shouldReturnFirst20Properties() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        mockMvc.perform(get("/api/v1/properties/")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(20)) // Assuming there are at least 20 properties
                .andExpect(jsonPath("$[0].id").exists()); // Check if at least first property has an ID
        TestUtil.testLogout(mockMvc, customerAccessToken);
    }

    @Test
    @Order(2)
    public void fetchAllProperties_withCustomPagination_shouldReturnPaginatedProperties() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        mockMvc.perform(get("/api/v1/properties/")
                        .header("Authorization", "Bearer " + customerAccessToken)
                        .param("pageNumber", "1")
                        .param("amount", "10")
                        .param("sortBy", "price")) // Sorting by 'price' field
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10)) // Expect 10 properties per page
                .andExpect(jsonPath("$[0].location").exists()); // Assuming 'name' is a valid property field
        TestUtil.testLogout(mockMvc, customerAccessToken);
    }

    @Test
    @Order(3)
    public void fetchAllProperties_withHighPageNumber_shouldReturnEmptyList() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        mockMvc.perform(get("/api/v1/properties/")
                        .header("Authorization", "Bearer " + customerAccessToken)
                        .param("pageNumber", "100")) // A high page number that has no properties
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0)); // Expect an empty list
        TestUtil.testLogout(mockMvc, customerAccessToken);
    }

    // 1b. Test Fetch Property by ID

    @Test
    @Order(4)
    public void fetchPropertyById_withValidId_shouldReturnProperty() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        Integer validPropertyId = 1; // Use a valid property ID from your test data
        mockMvc.perform(get("/api/v1/properties/{propertyId}", validPropertyId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(validPropertyId)) // Expect the returned property ID to match
                .andExpect(jsonPath("$.location").exists()); // Assuming 'name' is a valid property field
        TestUtil.testLogout(mockMvc, customerAccessToken);
    }

    @Test
    @Order(5)
    public void fetchPropertyById_withInvalidId_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        Integer invalidPropertyId = -2; // Use an invalid property ID that doesn't exist
        mockMvc.perform(get("/api/v1/properties/{propertyId}", invalidPropertyId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isNotFound());
        TestUtil.testLogout(mockMvc, customerAccessToken);
    }

    @Test
    @Order(6)
    public void addAndRemoveBookmark_forProperty_shouldWorkCorrectly() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        String s = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/user/")
                                .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk()
                ).andReturn().getResponse().getContentAsString();
        RetrieveAccountResponse customer = objectMapper.readValue(s, RetrieveAccountResponse.class);
        // Step 1: List all available properties and save the ID of the first result
        String propertyResponse = mockMvc.perform(get("/api/v1/properties/")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract propertyId from the response (assuming it's a JSON array of properties)
        Integer propertyId = objectMapper.readTree(propertyResponse).get(0).get("id").asInt();

        // Step 2: Add a bookmark for the property
        mockMvc.perform(post("/api/v1/customer/bookmarks/add/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Step 3: Verify that the bookmark was added using GET /bookmarks
        String bookmarkResponse = mockMvc.perform(get("/api/v1/customer/bookmarks")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].propertyId").value(propertyId))
                .andExpect(jsonPath("$[0].userId").value(customer.getId()))// Verify the bookmark is linked to the correct propertyId
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract bookmarkId from the response (assuming it returns an array of bookmarks)
        Integer bookmarkId = objectMapper.readTree(bookmarkResponse).get(0).get("id").asInt();
        assertThat(bookmarkId).isNotNull(); // Check that the bookmark exists

        // Step 4: Remove the bookmark
        mockMvc.perform(delete("/api/v1/customer/bookmarks/{bookmarkId}", bookmarkId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Step 5: Verify that the bookmark was removed using GET /bookmarks
        mockMvc.perform(get("/api/v1/customer/bookmarks")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // Ensure no bookmarks exist now
    }

    @Test
    @Order(7)
    public void addBookmark_forNonExistingProperty_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Define a non-existing propertyId
        Integer nonExistingPropertyId = -2; // This ID should not exist in your test database

        // Step 2: Try to add a bookmark for the non-existing property
        mockMvc.perform(post("/api/v1/customer/bookmarks/add/{propertyId}", nonExistingPropertyId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isNotFound()); // Assuming a specific error message, adjust if necessary
    }

    @Test
    @Order(8)
    public void removeBookmark_withInvalidBookmarkId_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Define a non-existing bookmarkId
        Integer nonExistingBookmarkId = -2; // This ID should not exist in your test database

        // Step 2: Try to remove the bookmark with the invalid bookmarkId
        mockMvc.perform(delete("/api/v1/customer/bookmarks/{bookmarkId}", nonExistingBookmarkId)
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isNotFound()); // Assuming a specific error message, adjust if necessary
    }

    @Test
    @Order(9)
    public void createFetchAndDeleteApplication_shouldWorkCorrectly() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String userAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Prepare application creation request data
        // Retrieve customer's profile
        String s = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        RetrieveProfileResponse originalProfile = objectMapper.readValue(s, RetrieveProfileResponse.class);
        // Convert JSON to RetrieveProfileResponse

        // Step 3: Admin updates customer's profile
        ApplicationRequest applicationRequest = new ApplicationRequest();
        BeanUtils.copyProperties(originalProfile, applicationRequest);
        if (applicationRequest.getPersonalDetails() == null) {
            applicationRequest.setPersonalDetails(new PersonalDetails());
        }
        if (applicationRequest.getContactDetails() == null) {
            applicationRequest.setContactDetails(new ContactDetails());
        }
        applicationRequest.getPersonalDetails().setFirstName("UpdatedFirstName");
        applicationRequest.getPersonalDetails().setLastName("UpdatedLastName");
        applicationRequest.getContactDetails().setContactNumber("1234567890");

        // Step 2: Create an application (POST /applications)
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",            // Part name
                "",       // Original filename (can be arbitrary)
                "application/json",    // Content type
                objectMapper.writeValueAsBytes(applicationRequest) // JSON content
        );
        MockMultipartFile image1 = new MockMultipartFile(
                "images",                // Part name (matching @RequestPart name)
                "image1.jpg",            // Filename
                "image/jpeg",            // Content type
                image1Bytes // File content
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "images",                // Part name (matching @RequestPart name)
                "image2.jpg",            // Filename
                "image/jpeg",            // Content type
                image2Bytes // File content
        );
        Integer propertyId = 1;
        s = mockMvc.perform(multipart("/api/v1/customer/applications/create/{propertyId}", propertyId)
                        .file(requestPart)
                        .file(image1)
                        .file(image2)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk()) // Expect success
                .andExpect(jsonPath("$.id").exists()) // Ensure the response has an ID for the created application
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApplicationCreationResponse applicationCreationResponse = objectMapper.readValue(s, ApplicationCreationResponse.class);


        // Extract application ID from the response (assuming it's in JSON format)
        Long applicationId = applicationCreationResponse.getId();

        // Step 3: Fetch the created application by ID (GET /applications/{applicationId})
        s = mockMvc.perform(get("/api/v1/customer/applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApplicationResponse applicationResponse = objectMapper.readValue(s, ApplicationResponse.class);
        Assertions.assertEquals(applicationResponse.getContactDetails().getContactNumber(), applicationRequest.getContactDetails().getContactNumber());
        Assertions.assertEquals(applicationResponse.getContactDetails().getContactEmail(), applicationRequest.getContactDetails().getContactEmail());
        Assertions.assertEquals(applicationResponse.getPersonalDetails().getFirstName(), applicationRequest.getPersonalDetails().getFirstName());
        Assertions.assertEquals(applicationResponse.getPersonalDetails().getLastName(), applicationRequest.getPersonalDetails().getLastName());

        // Step 4: Fetch all applications for the property (GET /properties/{propertyId}/applications)
        mockMvc.perform(get("/api/v1/customer/applications", propertyId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()) // Expect an array of applications
                .andExpect(jsonPath("$[?(@.id == " + applicationId + ")]").exists()); // Verify the created application is in the list

        // Step 5: Delete the application (DELETE /applications/{applicationId})
        mockMvc.perform(delete("/api/v1/customer/applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().is2xxSuccessful()); // Expect 204 No Content after deletion

        // Step 6: Verify the application was deleted (GET /applications/{applicationId})
        mockMvc.perform(get("/api/v1/customer/applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().is4xxClientError()); // Expect 404 Not Found after deletion
    }

    @Test
    @Order(10)
    public void createApplication_withInvalidPropertyId_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String userAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Prepare application creation request with an invalid propertyId
        ApplicationRequest applicationRequest = new ApplicationRequest();
        String createApplicationJson = objectMapper.writeValueAsString(applicationRequest);

        // Step 2: Send POST request and expect 404 Not Found
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createApplicationJson))
                .andExpect(status().isNotFound()); // Customize based on error response
    }

    @Test
    @Order(11)
    public void fetchApplication_withInvalidApplicationId_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String userAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Use a non-existent applicationId
        Integer invalidApplicationId = -2;

        // Step 2: Send GET request and expect 404 Not Found
        mockMvc.perform(get("/api/v1/applications/{applicationId}", invalidApplicationId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isNotFound()); // Customize based on error response
    }

    @Test
    @Order(12)
    public void deleteApplication_withInvalidApplicationId_shouldReturnNotFound() throws Exception {
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String userAccessToken = customerAuthResponse.getAccessToken();
        // Step 1: Use a non-existent applicationId
        Integer invalidApplicationId = -2;

        // Step 2: Send DELETE request and expect 404 Not Found
        mockMvc.perform(delete("/api/v1/customer/applications/{applicationId}", invalidApplicationId)
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isNotFound()); // Customize based on error response
    }
}
