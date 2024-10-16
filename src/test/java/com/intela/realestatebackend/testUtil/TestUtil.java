package com.intela.realestatebackend.testUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intela.realestatebackend.models.archetypes.Role;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.testUsers.TestUser;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestUtil {
    public static final String TEST_IMAGE_PATH = "/usr/src/files/images";

    public static byte[] readFileToBytes(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    public static List<IDImageResponse> getUploadedProfileIds(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/user/profile/ids")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseContent, new TypeReference<List<IDImageResponse>>() {});
    }

    public static IDImageResponse getUploadedProfileId(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String fileName) throws Exception {
        List<IDImageResponse> uploadedFiles = getUploadedProfileIds(mockMvc, objectMapper, accessToken);

        return uploadedFiles.stream()
                .filter(file -> fileName.equals(file.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileName));
    }

    public static void clearAllProfileIdFiles(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken) throws Exception {
        // Step 1: Retrieve all uploaded profile ID files
        List<IDImageResponse> uploadedFiles = getUploadedProfileIds(mockMvc, objectMapper, accessToken);

        // Step 2: Loop through each file and delete it one by one
        for (IDImageResponse file : uploadedFiles) {
            mockMvc.perform(delete("/api/v1/user/profile/ids/{idId}", file.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }
    }

    public static void verifyUploadedProfileId(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String fileName, byte[] originalBytes) throws Exception {
        // Step 1: Retrieve the list of images
        MvcResult result = mockMvc.perform(get("/api/v1/user/profile/ids")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // Assuming the response is a list of file metadata (could be URLs or file details)
        String responseContent = result.getResponse().getContentAsString();
        List<IDImageResponse> fileResponses = objectMapper.readValue(responseContent, new TypeReference<List<IDImageResponse>>() {});

        // Step 2: Find the image with the given file name
        IDImageResponse targetFile = fileResponses.stream()
                .filter(file -> fileName.equals(file.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileName));

        // Step 3: Retrieve the actual file bytes
        MvcResult fileResult = mockMvc.perform(get("/api/v1/user/profile/ids/{idId}", targetFile.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] retrievedBytes = fileResult.getResponse().getContentAsByteArray();

        // Step 4: Compare the retrieved bytes with the original file bytes
        Assertions.assertArrayEquals(originalBytes, retrievedBytes, "The uploaded and retrieved file contents should match");
        Assertions.assertArrayEquals(originalBytes, targetFile.getImage(), "The retrieved file contents from retrieve all and retrieve by id should match");

    }

    public static void verifyUploadedApplicationId(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, Integer propertyId, String fileName, byte[] originalBytes) throws Exception {
        List<IDImageResponse> uploadedFiles = getUploadedApplicationIds(mockMvc, objectMapper, accessToken, propertyId);
        IDImageResponse targetFile = uploadedFiles.stream()
                .filter(file -> fileName.equals(file.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileName));

        MvcResult fileResult = mockMvc.perform(get("/api/v1/applications/{propertyId}/ids/{idId}", propertyId, targetFile.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] retrievedBytes = fileResult.getResponse().getContentAsByteArray();
        Assertions.assertArrayEquals(originalBytes, retrievedBytes, "The uploaded and retrieved file contents should match");
    }

    public static List<IDImageResponse> getUploadedApplicationIds(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, Integer propertyId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/applications/{propertyId}/ids", propertyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseContent, new TypeReference<List<IDImageResponse>>() {});
    }


    public static ApplicationResponse getApplicationByIdAsDealer(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, Long applicationId) throws Exception {
        // Perform GET request to retrieve application by ID
        MvcResult result = mockMvc.perform(get("/api/v1/dealer/applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())  // Expect HTTP 200 OK response
                .andReturn();

        // Deserialize the response into ApplicationResponse class
        String content = result.getResponse().getContentAsString();
        return objectMapper.readValue(content, ApplicationResponse.class);
    }

    public static ApplicationResponse getApplicationByIdAsCustomer(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, Long applicationId) throws Exception {
        // Perform GET request to retrieve application by ID
        MvcResult result = mockMvc.perform(get("/api/v1/customer/applications/{applicationId}", applicationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())  // Expect HTTP 200 OK response
                .andReturn();

        // Deserialize the response into ApplicationResponse class
        String content = result.getResponse().getContentAsString();
        return objectMapper.readValue(content, ApplicationResponse.class);
    }

    public static void resetTestUserAccountInfo(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, TestUser testUser) throws Exception {
        UpdateAccountRequest updateAccountRequest = new UpdateAccountRequest(
                testUser.getFIRST_NAME(),
                testUser.getLAST_NAME(),
                testUser.getMOBILE_NUMBER(),
                testUser.getEMAIL()
        );
        mockMvc.perform(post("/api/v1/user/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAccountRequest)))
                .andExpect(status().isOk())
                .andReturn();
    }

    public static AuthenticationResponse testLogin(MockMvc mockMvc, ObjectMapper objectMapper, String email, String password) throws Exception {
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail(email);
        authRequest.setPassword(password);

        String authResponse = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is(202))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the access token from the response
        return objectMapper.readValue(authResponse, AuthenticationResponse.class);
    }

    public static void testLogout(MockMvc mockMvc, String accessToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());  // Logout should be successful
    }

    public static List<TestUser> testRegisterCustomerUsers(MockMvc mockMvc, ObjectMapper objectMapper, List<TestUser> allUsers) throws Exception {
        List<TestUser> testUserList = new ArrayList<>();
        for (TestUser user : allUsers) {
            if (user.getROLE().equals(Role.CUSTOMER)) {
                TestUtil.testRegister(mockMvc, objectMapper, user);
                testUserList.add(user);
            }
        }
        return testUserList;
    }

    public static List<TestUser> testRegisterDealerUsers(MockMvc mockMvc, ObjectMapper objectMapper, List<TestUser> allUsers) throws Exception {
        List<TestUser> testUserList = new ArrayList<>();
        for (TestUser user : allUsers) {
            if (user.getROLE().equals(Role.DEALER)) {
                TestUtil.testRegister(mockMvc, objectMapper, user);
                testUserList.add(user);
            }
        }
        return testUserList;
    }

    public static List<TestUser> testRegisterAdminUsers(MockMvc mockMvc, ObjectMapper objectMapper, List<TestUser> allUsers) throws Exception {
        List<TestUser> testUserList = new ArrayList<>();
        for (TestUser user : allUsers) {
            if (user.getROLE().equals(Role.ADMIN)) {
                TestUtil.testRegister(mockMvc, objectMapper, user);
                testUserList.add(user);
            }
        }
        return testUserList;
    }

    public static void testRegister(MockMvc mockMvc, ObjectMapper objectMapper, TestUser testUser) throws Exception {
        // Step 1: Prepare RegisterRequest object with all required fields
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName(testUser.getFIRST_NAME());
        registerRequest.setLastName(testUser.getLAST_NAME());
        registerRequest.setPassword(testUser.getPASSWORD());
        registerRequest.setEmail(testUser.getEMAIL());
        registerRequest.setMobileNumber(testUser.getMOBILE_NUMBER());
        registerRequest.setRole(testUser.getROLE());

        // Step 2: Perform POST /register without any specific role
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());  // Assuming successful registration returns HTTP 201
    }

    public static void testResetPasswordAndLogout(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String newPassword) throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setNewPassword(newPassword);

        mockMvc.perform(post("/api/v1/auth/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is(202))
                .andReturn();

        testLogout(mockMvc, accessToken);
    }
}
