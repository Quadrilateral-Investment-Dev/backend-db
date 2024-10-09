package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.models.profile.ContactDetails;
import com.intela.realestatebackend.requestResponse.AuthenticationResponse;
import com.intela.realestatebackend.requestResponse.IDImageResponse;
import com.intela.realestatebackend.requestResponse.RetrieveProfileResponse;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Paths;
import java.util.List;

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


}