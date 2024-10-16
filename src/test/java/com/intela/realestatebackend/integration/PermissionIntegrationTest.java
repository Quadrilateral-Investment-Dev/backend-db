package com.intela.realestatebackend.integration;

import com.intela.realestatebackend.BaseTestContainerTest;
import com.intela.realestatebackend.testUsers.TestUser;
import com.intela.realestatebackend.testUtil.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.List;

import static com.intela.realestatebackend.testUtil.TestUtil.IMAGES_PATH;
import static com.intela.realestatebackend.testUtil.TestUtil.cleanDirectory;

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
    @Order(1)
    void shouldRegisterUser() throws Exception {
        customerUsers = TestUtil.testRegisterCustomerUsers(mockMvc, objectMapper, allUsers);
        adminUsers = TestUtil.testRegisterAdminUsers(mockMvc, objectMapper, allUsers);
        dealerUsers = TestUtil.testRegisterDealerUsers(mockMvc, objectMapper, allUsers);
    }
}
