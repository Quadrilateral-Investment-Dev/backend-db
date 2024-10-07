package com.intela.realestatebackend.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.archetypes.ApplicationStatus;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PaymentCycle;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import com.intela.realestatebackend.models.profile.ContactDetails;
import com.intela.realestatebackend.models.profile.PersonalDetails;
import com.intela.realestatebackend.models.property.Feature;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.parameters.P;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CustomerDealerIntegrationTest extends BaseTestContainerTest {
    private static List<TestUser> dealerUsers;
    private static List<TestUser> adminUsers;
    private static List<TestUser> customerUsers;
    @Autowired
    private List<TestUser> allUsers;

    @Test
    @Order(1)
    void shouldRegisterUser() throws Exception {
        dealerUsers = TestUtil.testRegisterDealerUsers(mockMvc, objectMapper, allUsers);
        adminUsers = TestUtil.testRegisterAdminUsers(mockMvc, objectMapper, allUsers);
        customerUsers = TestUtil.testRegisterCustomerUsers(mockMvc, objectMapper, allUsers);
    }

    @Test
    @Order(2)
    void shouldDealerCreatePropertyCustomerApplyAndDealerApprove() throws Exception {
        // Step 1: Dealer logs in and creates a property
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Mock image files
        MockMultipartFile imageFile1 = new MockMultipartFile("images", "image1.png", "image/png", image1Bytes);
        MockMultipartFile imageFile2 = new MockMultipartFile("images", "image2.png", "image/png", image2Bytes);

        // Create property request
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Dealer")
                .location("456 Elm St, Sydney")
                .description("A beautiful house")
                .feature(feature)
                .propertyType(PropertyType.HOUSE)
                .price(750L)
                .paymentCycle(PaymentCycle.MONTHLY)
                .billType(BillType.NOT_INCLUDED)
                .build();

        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Dealer creates property
        MvcResult propertyCreationResult = mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Extract property ID from response
        PropertyCreationResponse propertyResponse = objectMapper.readValue(propertyCreationResult.getResponse().getContentAsString(), PropertyCreationResponse.class);
        Integer propertyId = propertyResponse.getId();

        // Step 2: Customer logs in and applies for the property
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        ApplicationRequest applicationRequest = new ApplicationRequest();
        applicationRequest.setContactDetails(new ContactDetails());
        applicationRequest.setPersonalDetails(new PersonalDetails());
        applicationRequest.setMessage("I would like to apply for this property.");
        applicationRequest.getPersonalDetails().setFirstName("UpdatedFirstName");
        applicationRequest.getPersonalDetails().setLastName("UpdatedLastName");
        applicationRequest.getContactDetails().setContactNumber("1234567890");

        String applicationRequestJson = objectMapper.writeValueAsString(applicationRequest);

        // Customer applies to the property
        MvcResult applicationCreationResult = mockMvc.perform(multipart("/api/v1/customer/applications/create/{propertyId}", propertyId)
                        .file(new MockMultipartFile("request", "", "application/json", applicationRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + customerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        ApplicationCreationResponse applicationCreationResponse = objectMapper.readValue(applicationCreationResult.getResponse().getContentAsString(), ApplicationCreationResponse.class);
        // Step 3: Dealer approves the application
        Long applicationId = applicationCreationResponse.getId(); // Utility to get the application ID

        // Dealer approves application
        mockMvc.perform(post("/api/v1/dealer/applications/approve/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());

        // Optional: Verify the status of the application is "approved"
        ApplicationResponse applicationResponse = TestUtil.getApplicationByIdAsCustomer(mockMvc, objectMapper, customerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.APPROVED, applicationResponse.getStatus());

        applicationResponse = TestUtil.getApplicationByIdAsDealer(mockMvc, objectMapper, dealerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.APPROVED, applicationResponse.getStatus());
    }

    @Test
    @Order(3)
    void shouldDealerCreatePropertyCustomerApplyAndDealerReject() throws Exception {
        // Step 1: Dealer logs in and creates a property
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Mock image files
        MockMultipartFile imageFile1 = new MockMultipartFile("images", "image1.png", "image/png", image1Bytes);
        MockMultipartFile imageFile2 = new MockMultipartFile("images", "image2.png", "image/png", image2Bytes);

        // Create property request
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Dealer")
                .location("456 Elm St, Sydney")
                .description("A beautiful house")
                .feature(feature)
                .propertyType(PropertyType.HOUSE)
                .price(750L)
                .paymentCycle(PaymentCycle.MONTHLY)
                .billType(BillType.NOT_INCLUDED)
                .build();

        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Dealer creates property
        MvcResult propertyCreationResult = mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Extract property ID from response
        PropertyCreationResponse propertyResponse = objectMapper.readValue(propertyCreationResult.getResponse().getContentAsString(), PropertyCreationResponse.class);
        Integer propertyId = propertyResponse.getId();

        // Step 2: Customer logs in and applies for the property
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        ApplicationRequest applicationRequest = new ApplicationRequest();
        applicationRequest.setContactDetails(new ContactDetails());
        applicationRequest.setPersonalDetails(new PersonalDetails());
        applicationRequest.setMessage("I would like to apply for this property.");
        applicationRequest.getPersonalDetails().setFirstName("UpdatedFirstName");
        applicationRequest.getPersonalDetails().setLastName("UpdatedLastName");
        applicationRequest.getContactDetails().setContactNumber("1234567890");

        String applicationRequestJson = objectMapper.writeValueAsString(applicationRequest);

        // Customer applies to the property
        MvcResult applicationCreationResult = mockMvc.perform(multipart("/api/v1/customer/applications/create/{propertyId}", propertyId)
                        .file(new MockMultipartFile("request", "", "application/json", applicationRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + customerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        ApplicationCreationResponse applicationCreationResponse = objectMapper.readValue(applicationCreationResult.getResponse().getContentAsString(), ApplicationCreationResponse.class);
        // Step 3: Dealer approves the application
        Long applicationId = applicationCreationResponse.getId(); // Utility to get the application ID

        // Dealer approves application
        mockMvc.perform(post("/api/v1/dealer/applications/reject/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());

        // Optional: Verify the status of the application is "approved"
        ApplicationResponse applicationResponse = TestUtil.getApplicationByIdAsCustomer(mockMvc, objectMapper, customerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.REJECTED, applicationResponse.getStatus());

        applicationResponse = TestUtil.getApplicationByIdAsDealer(mockMvc, objectMapper, dealerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.REJECTED, applicationResponse.getStatus());
    }

    @Test
    @Order(4)
    void shouldDealerCreatePropertyCustomerApplyAndToggleApplicationStatus() throws Exception {
        // Step 1: Dealer logs in and creates a property
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Mock image files
        MockMultipartFile imageFile1 = new MockMultipartFile("images", "image1.png", "image/png", image1Bytes);
        MockMultipartFile imageFile2 = new MockMultipartFile("images", "image2.png", "image/png", image2Bytes);

        // Create property request
        Feature feature = new Feature();
        feature.setBathrooms(2);
        feature.setBedrooms(2);
        feature.setLounges(1);
        feature.setParking(1);
        PropertyRequest propertyRequest = PropertyRequest.builder()
                .propertyOwnerName("John Dealer")
                .location("456 Elm St, Sydney")
                .description("A beautiful house")
                .feature(feature)
                .propertyType(PropertyType.HOUSE)
                .price(750L)
                .paymentCycle(PaymentCycle.MONTHLY)
                .billType(BillType.NOT_INCLUDED)
                .build();

        String propertyRequestJson = objectMapper.writeValueAsString(propertyRequest);

        // Dealer creates property
        MvcResult propertyCreationResult = mockMvc.perform(multipart("/api/v1/dealer/property/add")
                        .file(new MockMultipartFile("request", "", "application/json", propertyRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Extract property ID from response
        PropertyCreationResponse propertyResponse = objectMapper.readValue(propertyCreationResult.getResponse().getContentAsString(), PropertyCreationResponse.class);
        Integer propertyId = propertyResponse.getId();
        Assertions.assertNotNull(propertyId, "Property ID should not be null after creation.");

        // Step 2: Customer logs in and applies for the property
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        ApplicationRequest applicationRequest = new ApplicationRequest();
        applicationRequest.setContactDetails(new ContactDetails());
        applicationRequest.setPersonalDetails(new PersonalDetails());
        applicationRequest.setMessage("I would like to apply for this property.");
        applicationRequest.getPersonalDetails().setFirstName("UpdatedFirstName");
        applicationRequest.getPersonalDetails().setLastName("UpdatedLastName");
        applicationRequest.getContactDetails().setContactNumber("1234567890");

        String applicationRequestJson = objectMapper.writeValueAsString(applicationRequest);

        // Customer applies to the property
        MvcResult applicationCreationResult = mockMvc.perform(multipart("/api/v1/customer/applications/create/{propertyId}", propertyId)
                        .file(new MockMultipartFile("request", "", "application/json", applicationRequestJson.getBytes()))
                        .file(imageFile1)
                        .file(imageFile2)
                        .header("Authorization", "Bearer " + customerAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ApplicationCreationResponse applicationCreationResponse = objectMapper.readValue(applicationCreationResult.getResponse().getContentAsString(), ApplicationCreationResponse.class);
        Long applicationId = applicationCreationResponse.getId();
        Assertions.assertNotNull(applicationId, "Application ID should not be null after applying.");

        // Step 3: Dealer confirms the application is submitted by getting a list of all applications by propertyId
        MvcResult applicationsListResult = mockMvc.perform(get("/api/v1/dealer/applications/property/{propertyId}", propertyId)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        List<ApplicationResponse> applicationsList = objectMapper.readValue(applicationsListResult.getResponse().getContentAsString(), new TypeReference<List<ApplicationResponse>>() {});
        Assertions.assertFalse(applicationsList.isEmpty(), "Applications list should not be empty for the property.");

        ApplicationResponse submittedApplication = applicationsList.stream()
                .filter(app -> app.getId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Submitted application not found in list."));

        // Verify the current status is UNREAD
        Assertions.assertEquals(ApplicationStatus.UNREAD, submittedApplication.getStatus(), "Application status should be UNREAD initially.");

        // Step 4: Dealer gets the application by its id and confirms the status is now switched to READ
        ApplicationResponse applicationResponse = TestUtil.getApplicationByIdAsDealer(mockMvc, objectMapper, dealerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.READ, applicationResponse.getStatus(), "Application status should be READ after viewing.");

        // Step 5: Dealer marks the application as unread again
        mockMvc.perform(post("/api/v1/dealer/applications/unread/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + dealerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());

        // Step 6: Dealer gets the application again and confirms it's now UNREAD
        applicationResponse = TestUtil.getApplicationByIdAsDealer(mockMvc, objectMapper, dealerAccessToken, applicationId);
        Assertions.assertEquals(ApplicationStatus.UNREAD, applicationResponse.getStatus(), "Application status should be UNREAD after marking it as unread.");
    }
}
