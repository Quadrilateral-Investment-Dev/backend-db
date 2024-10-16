package com.intela.realestatebackend.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.profile.ContactDetails;
import com.intela.realestatebackend.requestResponse.AuthenticationResponse;
import com.intela.realestatebackend.requestResponse.IDImageResponse;
import com.intela.realestatebackend.requestResponse.PropertyResponse;
import com.intela.realestatebackend.requestResponse.RetrieveProfileResponse;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.intela.realestatebackend.testUtil.TestUtil.IMAGES_PATH;
import static com.intela.realestatebackend.testUtil.TestUtil.cleanDirectory;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FileUploadIntegrationTest extends BaseTestContainerTest {
    private static List<TestUser> adminUsers;
    private static List<TestUser> customerUsers;
    private static List<TestUser> dealerUsers;
    @Autowired
    private List<TestUser> allUsers;

    @AfterAll
    public static void cleanUp() throws IOException {
        // Delete all files in the ./resources/images directory
        cleanDirectory(IMAGES_PATH);
    }

    @Test
    @Order(0)
    void shouldRegisterUser() throws Exception {
        customerUsers = TestUtil.testRegisterCustomerUsers(mockMvc, objectMapper, allUsers);
        adminUsers = TestUtil.testRegisterAdminUsers(mockMvc, objectMapper, allUsers);
        dealerUsers = TestUtil.testRegisterDealerUsers(mockMvc, objectMapper, allUsers);
    }

    //Profile ID management tests
    @Test
    @Order(1)
    void shouldUploadAndRetrieveProfileIdFiles() throws Exception {
        // Simulate a user login and get an access token (Assuming a working login method)
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Read files from disk
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        byte[] image3Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image3.jpg").toString());

        // Upload the files using MockMvc
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image1Bytes))
                        .file(new MockMultipartFile("images", "image2.jpg", "image/jpeg", image2Bytes))
                        .file(new MockMultipartFile("images", "image3.jpg", "image/jpeg", image3Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Retrieve and verify uploaded files
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image1.jpg", image1Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image2.jpg", image2Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image3.jpg", image3Bytes);
        TestUtil.clearAllProfileIdFiles(mockMvc, objectMapper, accessToken);
    }

    @Test
    @Order(3)
    void shouldUpdateProfileAndRetrieveProfileIdFiles() throws Exception {
        // Simulate a user login and get an access token (Assuming a working login method)
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        //Profile update request
        String s = mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        RetrieveProfileResponse retrieveProfileResponse = objectMapper.readValue(s, RetrieveProfileResponse.class);
        assertNull(retrieveProfileResponse.getContactDetails());

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setContactEmail(customerUsers.get(0).getEMAIL());
        contactDetails.setContactNumber(customerUsers.get(0).getMOBILE_NUMBER());

        retrieveProfileResponse.setContactDetails(contactDetails);
        s = objectMapper.writeValueAsString(retrieveProfileResponse);
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",            // Part name
                "",       // Original filename (can be arbitrary)
                "application/json",    // Content type
                s.getBytes() // JSON content
        );

        // Read files from disk
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        byte[] image3Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image3.jpg").toString());

        // Upload the files using MockMvc
        mockMvc.perform(multipart("/api/v1/user/profile")
                        .file(requestPart)
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image1Bytes))
                        .file(new MockMultipartFile("images", "image2.jpg", "image/jpeg", image2Bytes))
                        .file(new MockMultipartFile("images", "image3.jpg", "image/jpeg", image3Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Retrieve and verify uploaded files
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image1.jpg", image1Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image2.jpg", image2Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image3.jpg", image3Bytes);
        TestUtil.clearAllProfileIdFiles(mockMvc, objectMapper, accessToken);
    }

    @Test
    @Order(4)
    void shouldHandleDuplicateFileNamesAndCorrectlyUploadFiles() throws Exception {
        // Step 1: Simulate a user login and get an access token
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Read files from disk
        byte[] image1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image1.jpg").toString());
        byte[] image2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image2.jpg").toString());
        byte[] image3Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "image3.jpg").toString());

        // Step 2: Upload three files, two of them having the same name (image1.jpg twice), expecting an error
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image1Bytes))
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image1Bytes))  // Duplicate name
                        .file(new MockMultipartFile("images", "image2.jpg", "image/jpeg", image2Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());  // Expecting an error due to duplicate filenames

        // Step 3: Verify that no files were uploaded because of the error
        List<IDImageResponse> uploadedFiles = TestUtil.getUploadedProfileIds(mockMvc, objectMapper, accessToken);
        Assertions.assertTrue(uploadedFiles.isEmpty(), "No files should be uploaded due to the error");

        // Step 4: Now upload two files with the same name separately
        // Upload first file
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image1Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Try uploading the second file with the same name (expecting this to replace or ignore based on implementation)
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("images", "image1.jpg", "image/jpeg", image2Bytes))  // Same name as before, but different content
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Step 5: Verify that only the first instance of "image1.jpg" has been saved
        IDImageResponse firstImage = TestUtil.getUploadedProfileId(mockMvc, objectMapper, accessToken, "image1.jpg");
        Assertions.assertArrayEquals(image1Bytes, firstImage.getImage(), "The first uploaded file should be saved");

        // Step 6: Now upload three different files with three different names and verify all files are uploaded correctly
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("images", "image3.jpg", "image/jpeg", image3Bytes))
                        .file(new MockMultipartFile("images", "image2.jpg", "image/jpeg", image2Bytes))
                        .file(new MockMultipartFile("images", "image4.jpg", "image/jpeg", image1Bytes))  // Different name, same content as image1Bytes
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Verify all files have been uploaded correctly
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image1.jpg", image1Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image2.jpg", image2Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image3.jpg", image3Bytes);
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "image4.jpg", image1Bytes);  // Different name, but same content as first file
        TestUtil.clearAllProfileIdFiles(mockMvc, objectMapper, accessToken);
    }

    @Test
    @Order(5)
    void shouldFailUploadingTxtAndSucceedUploadingPdf() throws Exception {
        // Simulate a user login and get an access token
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Read .txt and .pdf files from disk
        byte[] txtFileBytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "sample.txt").toString());
        byte[] pdfFileBytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "sample.pdf").toString());

        // Step 1: Attempt to upload a .txt file (expect failure)
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("file", "sample.txt", "text/plain", txtFileBytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());  // Expect failure due to invalid file type

        // Step 2: Upload a .pdf file (expect success)
        mockMvc.perform(multipart("/api/v1/user/profile/ids")
                        .file(new MockMultipartFile("file", "sample.pdf", "application/pdf", pdfFileBytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());  // Expect success

        // Step 3: Verify the PDF file was uploaded
        TestUtil.verifyUploadedProfileId(mockMvc, objectMapper, accessToken, "sample.pdf", pdfFileBytes);
    }

    //Application ID management tests
    @Test
    @Order(6)
    void shouldUploadAndRetrieveApplicationIdFiles() throws Exception {
        // Simulate a user login and get an access token
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Step 1: Retrieve the first property from the list
        MvcResult propertyResult = mockMvc.perform(get("/api/v1/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String propertyResponse = propertyResult.getResponse().getContentAsString();
        List<PropertyResponse> propertyList = objectMapper.readValue(propertyResponse, new TypeReference<List<PropertyResponse>>() {});
        Integer propertyId = propertyList.get(0).getId();  // Get the ID of the first property

        // Read files from disk
        byte[] application1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application1.pdf").toString());
        byte[] application2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application2.pdf").toString());
        byte[] application3Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application3.pdf").toString());

        // Step 2: Upload the files using MockMvc
        mockMvc.perform(multipart("/api/v1/applications/{propertyId}/ids", propertyId)
                        .file(new MockMultipartFile("files", "application1.pdf", "application/pdf", application1Bytes))
                        .file(new MockMultipartFile("files", "application2.pdf", "application/pdf", application2Bytes))
                        .file(new MockMultipartFile("files", "application3.pdf", "application/pdf", application3Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Step 3: Retrieve and verify uploaded files
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application1.pdf", application1Bytes);
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application2.pdf", application2Bytes);
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application3.pdf", application3Bytes);
    }

    @Test
    @Order(7)
    void shouldHandleDuplicateFileNamesForApplicationsAndCorrectlyUploadFiles() throws Exception {
        // Simulate a user login and get an access token
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Retrieve the first property from the list
        MvcResult propertyResult = mockMvc.perform(get("/api/v1/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String propertyResponse = propertyResult.getResponse().getContentAsString();
        List<PropertyResponse> propertyList = objectMapper.readValue(propertyResponse, new TypeReference<List<PropertyResponse>>() {});
        Integer propertyId = propertyList.get(0).getId();  // Get the ID of the first property

        // Read files from disk
        byte[] application1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application1.pdf").toString());
        byte[] application2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application2.pdf").toString());

        // Step 1: Attempt to upload two files with the same name (application1.pdf), expecting an error
        mockMvc.perform(multipart("/api/v1/applications/{propertyId}/ids", propertyId)
                        .file(new MockMultipartFile("files", "application1.pdf", "application/pdf", application1Bytes))
                        .file(new MockMultipartFile("files", "application1.pdf", "application/pdf", application1Bytes))  // Duplicate name
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());  // Expecting an error due to duplicate filenames

        // Step 2: Verify that no files were uploaded due to the error
        List<IDImageResponse> uploadedFiles = TestUtil.getUploadedApplicationIds(mockMvc, objectMapper, accessToken, propertyId);
        Assertions.assertTrue(uploadedFiles.isEmpty(), "No files should be uploaded due to the error");

        // Step 3: Now upload two files with the same name separately
        mockMvc.perform(multipart("/api/v1/applications/{propertyId}/ids", propertyId)
                        .file(new MockMultipartFile("files", "application1.pdf", "application/pdf", application1Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Try uploading the second file with the same name (application2.pdf)
        mockMvc.perform(multipart("/api/v1/applications/{propertyId}/ids", propertyId)
                        .file(new MockMultipartFile("files", "application2.pdf", "application/pdf", application2Bytes))  // Same name, different content
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Step 4: Verify both files were uploaded correctly
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application1.pdf", application1Bytes);
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application2.pdf", application2Bytes);
    }

    @Test
    @Order(8)
    void shouldUploadAndRetrieveMultipleApplicationIdFiles() throws Exception {
        // Simulate a user login and get an access token
        AuthenticationResponse authResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String accessToken = authResponse.getAccessToken();

        // Retrieve the first property from the list
        MvcResult propertyResult = mockMvc.perform(get("/api/v1/properties")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String propertyResponse = propertyResult.getResponse().getContentAsString();
        List<PropertyResponse> propertyList = objectMapper.readValue(propertyResponse, new TypeReference<List<PropertyResponse>>() {});
        Integer propertyId = propertyList.get(0).getId();  // Get the ID of the first property

        // Read files from disk
        byte[] application1Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application1.pdf").toString());
        byte[] application2Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application2.pdf").toString());
        byte[] application3Bytes = TestUtil.readFileToBytes(Paths.get(TestUtil.TEST_IMAGE_PATH, "application3.pdf").toString());

        // Step 1: Upload three different files with different names
        mockMvc.perform(multipart("/api/v1/applications/{propertyId}/ids", propertyId)
                        .file(new MockMultipartFile("files", "application1.pdf", "application/pdf", application1Bytes))
                        .file(new MockMultipartFile("files", "application2.pdf", "application/pdf", application2Bytes))
                        .file(new MockMultipartFile("files", "application3.pdf", "application/pdf", application3Bytes))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Step 2: Verify all files have been uploaded correctly
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application1.pdf", application1Bytes);
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application2.pdf", application2Bytes);
        TestUtil.verifyUploadedApplicationId(mockMvc, objectMapper, accessToken, propertyId, "application3.pdf", application3Bytes);
    }

}