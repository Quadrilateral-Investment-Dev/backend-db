package com.intela.realestatebackend.services;

import com.intela.realestatebackend.models.ProfileImage;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.profile.ID;
import com.intela.realestatebackend.models.profile.Profile;
import com.intela.realestatebackend.repositories.ProfileImageRepository;
import com.intela.realestatebackend.repositories.ProfileRepository;
import com.intela.realestatebackend.repositories.UserRepository;
import com.intela.realestatebackend.repositories.application.IDRepository;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.util.Util;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intela.realestatebackend.util.Util.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private ProfileImageRepository profileImageRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UploadedFileService imageService;
    @Autowired
    private IDRepository idRepository;


    @Transactional
    public void updateProfile(HttpServletRequest servletRequest, MultipartFile[] images, UpdateProfileRequest request) throws IllegalAccessException {
        // Find the CustomerInformation associated with the userId where propertyId is null
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Update user details based on UpdateProfileRequest
        Profile profile = profileRepository.findByProfileOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));
        this.addIds(servletRequest, images);
        // Update user details based on UpdateProfileRequest
        Util.updateProfileFromRequest(profile, request);
        // Save the updated user
        profile.setRelationships();
        profileRepository.save(profile);
    }

    public void removeIdById(HttpServletRequest servletRequest, Integer idId) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        ID id = idRepository.findById(idId).orElseThrow(() -> new RuntimeException("ID not found"));
        if (id.getProfile().getProfileOwner().getId().equals(user.getId())) {
            idRepository.delete(id);
        }
    }

    public void addIds(HttpServletRequest servletRequest, MultipartFile[] images) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Set<ID> ids = new HashSet<>();
        Util.multipartFileToIDList(user.getId(), profileRepository, images, ids, imageService);
        user.getProfile().getIds().addAll(ids);
        userRepository.save(user);
    }

    public void clearIds(HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        idRepository.deleteAll(user.getProfile().getIds());
    }

    public UpdateAccountResponse updateAccount(HttpServletRequest servletRequest, UpdateAccountRequest request) throws IllegalAccessException {
        // Find the user by userId
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Update user details based on UpdateProfileRequest
        Map<String, Object> updatedFields = Util.updateAccountFromRequest(user, request);

        // Save the updated user
        userRepository.save(user);

        // Return the updated profile response
        return Util.mapToUpdateAccountResponse(updatedFields);
    }

    public RetrieveProfileResponse retrieveProfile(HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Profile profile = profileRepository.findByProfileOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));
        return new RetrieveProfileResponse(profile);
    }

    public ProfileImageResponse getProfileImage(HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        ProfileImage image = profileImageRepository.findByUserId(user.getId());
        ProfileImageResponse profileImageResponse = new ProfileImageResponse(image);
        Util.toFullImage(profileImageResponse);
        return profileImageResponse;
    }

    public void updateProfileImage(HttpServletRequest servletRequest, MultipartFile image) throws IOException {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        ProfileImage profileImage = Util.multipartFileToProfileImage(user,
                image, imageService);
        user.setProfileImage(profileImage);
        userRepository.save(user);
    }

    public RetrieveAccountResponse retrieveAccount(HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        return new RetrieveAccountResponse(user);
    }

    public List<IDImageResponse> getIdsByUserId(HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Profile profile = profileRepository.findByProfileOwnerId(user.getId()).orElseThrow(
                () -> new RuntimeException("Profile not found")
        );
        List<ID> idImageResponses = idRepository.findAllByProfileId(Math.toIntExact(profile.getId()));
        return idImageResponses.stream()
                .map(Util::convertFromIDImageToImageResponse) // Assuming ImageResponse has a constructor that takes a PropertyImage
                .collect(Collectors.toList());
    }

    public IDImageResponse getIdByIdId(Integer idId, HttpServletRequest servletRequest) {
        ID id = idRepository.findById(idId).orElseThrow(() -> new RuntimeException("ID not found"));
        return convertFromIDImageToImageResponse(id);
    }

    public void deleteIdByIdId(Integer idId, HttpServletRequest servletRequest) {
        // Step 1: Retrieve the ID file from the database
        ID idFile = idRepository.findById(idId)
                .orElseThrow(() -> new RuntimeException("ID file not found with id: " + idId));

        // Step 2: Remove the file from the filesystem
        try {
            imageService.removeFile(idFile.getPath()); // Adjust the parameters as necessary
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove file: " + e.getMessage());
        }

        // Step 3: Delete the ID record from the database
        idRepository.deleteById(idId);
    }

    public void addIdsToProfile(MultipartFile[] images, HttpServletRequest servletRequest) {
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Integer userId = user.getId();
        Set<ID> ids = new HashSet<>();
        Profile profile = this.profileRepository.findByProfileOwnerId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        multipartFileToIDList(profile.getProfileOwner().getId(), profileRepository, images, ids, imageService);

        //set images property id to saved property
        try {
            ids.forEach(id -> id.setProfile(profile));
            this.idRepository.saveAll(ids);
        } catch (Exception e) {
            throw new RuntimeException("Could not save image: " + e);
        }
    }
}
