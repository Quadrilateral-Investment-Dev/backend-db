package com.intela.realestatebackend.controllers;

import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.services.UserService;
import com.intela.realestatebackend.util.Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @Value(value = "${application.custom.maximum-file-size.property-images}")
    private Integer MAX_FILE_SIZE_PROPERTY_IMAGES;

    @Operation(
            summary = "Updates user profile",
            description = "Uploads a user profile JSON object",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            encoding = {
                                    @Encoding(name = "request", contentType = "application/json"),
                                    @Encoding(name = "images", contentType = "image/png, image/jpeg")
                            },
                            mediaType = "multipart/form-data",
                            schemaProperties =
                                    {
                                            @SchemaProperty(
                                                    name = "request",
                                                    schema = @Schema(implementation = UpdateProfileRequest.class)
                                            ),
                                            @SchemaProperty(
                                                    name = "images",
                                                    array = @ArraySchema(
                                                            schema = @Schema(type = "string", format = "binary")
                                                    )
                                            )
                                    }

                    )
            )
    )
    @PostMapping("/profile")
    public ResponseEntity<String> updateProfile(
            HttpServletRequest servletRequest,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart("request") UpdateProfileRequest request
    ) {
        try {
            userService.updateProfile(servletRequest, images, request);
            return ResponseEntity.ok().body("Profile updated successfully");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(
            summary = "Uploads profile image",
            description = "Uploads profile image",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            encoding = {
                                    @Encoding(name = "image", contentType = "image/png, image/jpeg")
                            },
                            mediaType = "multipart/form-data",
                            schemaProperties =
                                    {
                                            @SchemaProperty(
                                                    name = "image",
                                                    schema = @Schema(type = "string", format = "binary")
                                            )
                                    }

                    )
            )
    )
    @PostMapping("/profile/avatar")
    public ResponseEntity<String> updateProfileImage(
            HttpServletRequest servletRequest,
            @RequestPart MultipartFile image
    ) {
        try {
            userService.updateProfileImage(servletRequest, image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("Profile image updated");
    }

    @GetMapping("/profile/avatar")
    public ResponseEntity<ProfileImageResponse> getProfileImage(
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok().body(userService.getProfileImage(servletRequest));
    }

    @GetMapping("/profile")
    public ResponseEntity<RetrieveProfileResponse> retrieveProfile(
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok().body(userService.retrieveProfile(servletRequest));
    }

    @GetMapping(value = "/profile/ids")
    public ResponseEntity<List<IDImageResponse>> getIdsByUserId(HttpServletRequest servletRequest) {
        return ResponseEntity.ok(this.userService.getIdsByUserId(servletRequest));
    }

    @GetMapping(value = "/profile/ids/{idId}")
    public ResponseEntity<IDImageResponse> getIdByIdId(@PathVariable Integer idId, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(this.userService.getIdByIdId(idId, servletRequest));
    }

    @DeleteMapping(value = "/profile/ids/{idId}")
    public ResponseEntity<String> deleteIdByIdId(@PathVariable Integer idId, HttpServletRequest servletRequest) {
        this.userService.deleteIdByIdId(idId, servletRequest);
        return ResponseEntity.ok("ID file deleted");
    }

    @PostMapping("/profile/ids")
    public ResponseEntity<String> addIdsToProfile(@RequestPart MultipartFile[] images, HttpServletRequest servletRequest) {
        if (images != null) {
            for (MultipartFile multipartFile : images) {
                if (!Util.isImageOrPdf(multipartFile)) {
                    return ResponseEntity.status(415).body(null);
                }
                if (Util.exceedsSizeLimit(multipartFile, MAX_FILE_SIZE_PROPERTY_IMAGES)) {
                    return ResponseEntity.status(413).body(null);
                }
            }
        }
        this.userService.addIdsToProfile(images, servletRequest);
        return ResponseEntity.ok("Images added successfully");
    }

    @PostMapping("/")
    public ResponseEntity<UpdateAccountResponse> updateAccount(
            HttpServletRequest servletRequest,
            @RequestBody UpdateAccountRequest request
    ) {
        try {
            return ResponseEntity.ok().body(userService.updateAccount(servletRequest, request));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/")
    public ResponseEntity<RetrieveAccountResponse> retrieveAccount(
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok().body(userService.retrieveAccount(servletRequest));
    }
}
