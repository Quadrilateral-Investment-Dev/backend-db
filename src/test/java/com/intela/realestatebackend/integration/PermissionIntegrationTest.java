package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.requestResponse.AuthenticationResponse;
import com.intela.realestatebackend.requestResponse.UpdateAccountRequest;
import com.intela.realestatebackend.requestResponse.UpdateProfileRequest;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import static com.intela.realestatebackend.testUtil.TestUtil.IMAGES_PATH;
import static org.hamcrest.Matchers.not;
import static com.intela.realestatebackend.testUtil.TestUtil.cleanDirectory;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PermissionIntegrationTest extends BaseTestContainerTest {
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

    //Implemented Tests for AdminController

    @Test
    @Order(1)
    void shouldAdminUserAccessAdminEndpointsAndSucceed() throws Exception {
        // Admin logs in and retrieves token
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test /test endpoint
        mockMvc.perform(get("/api/v1/admin/test")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))  // Use Matchers.not() to assert that status is not 403 Forbidden
                .andExpect(status().is(not(401))); // Use Matchers.not() to assert that status is not 401 Unauthorized

        // Test listAllProfiles
        mockMvc.perform(get("/api/v1/admin/user-management/profiles")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test listAllAccounts
        mockMvc.perform(get("/api/v1/admin/user-management")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test deleteAccount
        mockMvc.perform(delete("/api/v1/admin/user-management/1")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test updateProfile (skip complex parameter validation)
        mockMvc.perform(multipart("/api/v1/admin/user-management/profiles/1")
                        .file("images", new byte[0])
                        .param("request", objectMapper.writeValueAsString(new UpdateProfileRequest()))
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test retrieveProfile
        mockMvc.perform(get("/api/v1/admin/user-management/profiles/1")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test getIdsByUserId
        mockMvc.perform(get("/api/v1/admin/user-management/profiles/ids/1")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test getIdByIdId
        mockMvc.perform(get("/api/v1/admin/user-management/profiles/ids/1/2")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test deleteIdByIdId
        mockMvc.perform(delete("/api/v1/admin/user-management/profiles/ids/1/2")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test addIdsToProfile
        mockMvc.perform(multipart("/api/v1/admin/user-management/profiles/ids/1")
                        .file("images", new byte[0])
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test updateAccount
        mockMvc.perform(post("/api/v1/admin/user-management/1")
                        .content(objectMapper.writeValueAsString(new UpdateAccountRequest(null, null, null, null)))
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType("application/json"))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test retrieveAccount
        mockMvc.perform(get("/api/v1/admin/user-management/1")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test banAccount
        mockMvc.perform(post("/api/v1/admin/user-management/ban/1")
                        .content(objectMapper.writeValueAsString(new Timestamp(System.currentTimeMillis() + 1000000)))
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType("application/json"))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));

        // Test unbanAccount
        mockMvc.perform(post("/api/v1/admin/user-management/unban/1")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }

    @Test
    @Order(2)
    void shouldCustomerAndDealerAccessAdminEndpointsAndFail() throws Exception {
        // Customer logs in and retrieves token
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Dealer logs in and retrieves token
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Test customer accessing admin endpoints (expect 403 Forbidden)
        mockMvc.perform(get("/api/v1/admin/test")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isForbidden());

        // Test dealer accessing admin endpoints (expect 403 Forbidden)
        mockMvc.perform(get("/api/v1/admin/test")
                        .header("Authorization", "Bearer " + dealerAccessToken))
                .andExpect(status().isForbidden());
    }

    // Test for Unauthenticated user accessing Admin endpoints
    @Test
    @Order(3)
    void shouldUnauthenticatedUserAccessAdminEndpointsAndFail() throws Exception {
        // Test unauthenticated access to admin endpoints (expect 401 Unauthorized)
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/user-management/profiles"))
                .andExpect(status().isForbidden());
    }

    //Implemented Tests for AuthController

    @Test
    @Order(4)
    void shouldCustomerAndDealerAccessAdminEndpointsAndFail1() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Dealer logs in
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Test admin-only endpoints with customer token
        mockMvc.perform(get("/api/v1/auth/userByAccessToken")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isForbidden());

        // Test admin-only endpoints with dealer token
        mockMvc.perform(get("/api/v1/auth/userByAccessToken")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dealerAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void shouldUnauthenticatedUserAccessAuthenticatedEndpointsAndFail1() throws Exception {
        // Test unauthenticated user trying to access "/user" (which requires authentication)
        mockMvc.perform(get("/api/v1/auth/user"))
                .andExpect(status().isForbidden());

        // Test unauthenticated user trying to access "/resetPassword" (requires authentication)
        mockMvc.perform(post("/api/v1/auth/resetPassword")
                        .contentType("application/json")
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void shouldUnauthenticatedUserAccessUnauthenticatedEndpointsAndSucceed1() throws Exception {
        // Test unauthenticated access to public endpoint (e.g., authenticate)
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType("application/json")
                        .content("{\"username\":\"testuser\", \"password\":\"testpassword\"}"))
                .andExpect(status().is(not(401)))  // Success should not return 401
                .andExpect(status().is(not(403))); // Success should not return 403
    }

    @Test
    @Order(7)
    void shouldAuthenticatedUserAccessUnauthenticatedEndpointsAndSucceed1() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Test authenticated user accessing unauthenticated endpoint (e.g., authenticate)
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken)
                        .contentType("application/json")
                        .content("{\"username\":\"testuser\", \"password\":\"testpassword\"}"))
                .andExpect(status().is(not(401)))  // Success should not return 401
                .andExpect(status().is(not(403))); // Success should not return 403
    }

    @Test
    @Order(8)
    void shouldCustomerAndDealerAndAdminUserAccessUserEndpointsAndSucceed() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Dealer logs in
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Admin logs in
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test "/user" endpoint with customer token
        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().is(not(401)))  // Should not be unauthorized
                .andExpect(status().is(not(403))); // Should not be forbidden

        // Test "/user" endpoint with dealer token
        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dealerAccessToken))
                .andExpect(status().is(not(401)))  // Should not be unauthorized
                .andExpect(status().is(not(403))); // Should not be forbidden

        // Test "/user" endpoint with admin token
        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().is(not(401)))  // Should not be unauthorized
                .andExpect(status().is(not(403))); // Should not be forbidden
    }

    @Test
    @Order(9)
    void shouldAdminUserAccessAdminEndpointsAndSucceed1() throws Exception {
        // Admin logs in
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test admin-only endpoint "/userByAccessToken" with admin token
        mockMvc.perform(get("/api/v1/auth/userByAccessToken")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().is(not(401)))  // Should not be unauthorized
                .andExpect(status().is(not(403))); // Should not be forbidden

        // Test admin-only endpoint "/userByRefreshToken" with admin token
        mockMvc.perform(get("/api/v1/auth/userByRefreshToken")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().is(not(401)))  // Should not be unauthorized
                .andExpect(status().is(not(403))); // Should not be forbidden
    }

    //Implemented Tests for CustomerController

    @Test
    @Order(10)
    void shouldUnauthenticatedUserAccessAuthenticatedEndpointsAndFail2() throws Exception {
        // Test: Unauthenticated user trying to fetch bookmarks (requires authentication)
        mockMvc.perform(get("/api/v1/customer/bookmarks"))
                .andExpect(status().isForbidden());

        // Test: Unauthenticated user trying to create an application (requires authentication)
        mockMvc.perform(post("/api/v1/customer/applications/create/1"))
                .andExpect(status().isForbidden());

        // Test: Unauthenticated user trying to get an application by ID (requires authentication)
        mockMvc.perform(get("/api/v1/customer/applications/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    void shouldCustomerAndDealerAndAdminUserAccessCustomerEndpointsAndSucceed2() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Dealer logs in
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Admin logs in
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test customer endpoints with customer token (e.g., fetch bookmarks)
        mockMvc.perform(get("/api/v1/customer/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Test customer endpoints with dealer token
        mockMvc.perform(get("/api/v1/customer/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dealerAccessToken))
                .andExpect(status().isOk());

        // Test customer endpoints with admin token
        mockMvc.perform(get("/api/v1/customer/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().isOk());

        // Test get application by ID with customer token
        mockMvc.perform(get("/api/v1/customer/applications/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().is(not(401)))  // Success should not return 401
                .andExpect(status().is(not(403)));
    }

    //Implemented Tests for DealerController
    @Test
    @Order(12)
    void shouldCustomerAccessDealerEndpointsAndFail() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Test dealer-only endpoints with customer token (e.g., add property)
        mockMvc.perform(post("/api/v1/dealer/property/add")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    void shouldUnauthenticatedUserAccessAuthenticatedEndpointsAndFail3() throws Exception {
        // Test: Unauthenticated user trying to access dealer-specific endpoints (requires authentication)
        mockMvc.perform(get("/api/v1/dealer/properties"))
                .andExpect(status().isForbidden());

        // Test: Unauthenticated user trying to create a property (requires authentication)
        mockMvc.perform(post("/api/v1/dealer/property/add"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    void shouldDealerAndAdminUserAccessDealerEndpointsAndSucceed() throws Exception {
        // Dealer logs in
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Admin logs in
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test dealer endpoints with dealer token (e.g., fetch all properties)
        mockMvc.perform(get("/api/v1/dealer/properties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dealerAccessToken))
                .andExpect(status().isOk());

        // Test dealer endpoints with admin token
        mockMvc.perform(get("/api/v1/dealer/properties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().isOk());
    }

    //Implemented tests for PropertyController
    @Test
    @Order(15)
    void shouldUnauthenticatedUserAccessUnauthenticatedEndpointsAndSucceed() throws Exception {
        // Test unauthenticated user can access the property list endpoint
        mockMvc.perform(get("/api/v1/properties/"))
                .andExpect(status().isOk());

        // Test unauthenticated user can access the fetch property by ID endpoint
        mockMvc.perform(get("/api/v1/properties/1"))
                .andExpect(status().isOk());

        // Test unauthenticated user can access the fetch property images by ID endpoint
        mockMvc.perform(get("/api/v1/properties/images/1"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    void shouldAuthenticatedUserAccessUnauthenticatedEndpointsAndSucceed() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Test authenticated user accessing unauthenticated endpoints (property list)
        mockMvc.perform(get("/api/v1/properties/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Test authenticated user accessing unauthenticated endpoints (fetch property by ID)
        mockMvc.perform(get("/api/v1/properties/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Test authenticated user accessing unauthenticated endpoints (fetch property images by ID)
        mockMvc.perform(get("/api/v1/properties/images/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isOk());
    }

    //Implemented tests for UserController

    @Test
    @Order(17)
    void shouldUnauthenticatedUserAccessAuthenticatedEndpointsAndFail() throws Exception {
        // Test: Unauthenticated user trying to access user profile (requires authentication)
        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isForbidden());

        // Test: Unauthenticated user trying to update profile (requires authentication)
        mockMvc.perform(post("/api/v1/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(18)
    void shouldCustomerAndDealerAndAdminUserAccessCustomerEndpointsAndSucceed() throws Exception {
        // Customer logs in
        AuthenticationResponse customerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, customerUsers.get(0).getEMAIL(), customerUsers.get(0).getPASSWORD());
        String customerAccessToken = customerAuthResponse.getAccessToken();

        // Dealer logs in
        AuthenticationResponse dealerAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, dealerUsers.get(0).getEMAIL(), dealerUsers.get(0).getPASSWORD());
        String dealerAccessToken = dealerAuthResponse.getAccessToken();

        // Admin logs in
        AuthenticationResponse adminAuthResponse = TestUtil.testLogin(mockMvc, objectMapper, adminUsers.get(0).getEMAIL(), adminUsers.get(0).getPASSWORD());
        String adminAccessToken = adminAuthResponse.getAccessToken();

        // Test user endpoints with customer token (e.g., retrieve profile)
        mockMvc.perform(get("/api/v1/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().isOk());

        // Test user endpoints with dealer token (e.g., retrieve profile)
        mockMvc.perform(get("/api/v1/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dealerAccessToken))
                .andExpect(status().isOk());

        // Test user endpoints with admin token (e.g., retrieve profile)
        mockMvc.perform(get("/api/v1/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                .andExpect(status().isOk());

        // Test authenticated users can update profile
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",            // Part name
                "",       // Original filename (can be arbitrary)
                "application/json",    // Content type
                (byte[]) null // JSON content
        );
        mockMvc.perform(multipart("/api/v1/user/profile")
                        .file(requestPart)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAccessToken))
                .andExpect(status().is(not(401)))  // Success should not return 401
                .andExpect(status().is(not(403)));
    }
}
