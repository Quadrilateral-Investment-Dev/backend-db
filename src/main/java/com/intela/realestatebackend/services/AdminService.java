package com.intela.realestatebackend.services;

import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.profile.ID;
import com.intela.realestatebackend.models.profile.Profile;
import com.intela.realestatebackend.repositories.ProfileRepository;
import com.intela.realestatebackend.repositories.UserRepository;
import com.intela.realestatebackend.repositories.application.IDRepository;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.util.Util;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.intela.realestatebackend.util.Util.convertFromIDImageToImageResponse;

@Service
@RequiredArgsConstructor
public class AdminService {

    @Autowired
    private final UploadedFileService imageService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private IDRepository idRepository;

    public List<RetrieveProfileResponse> listAllProfiles() {
        // Retrieve profiles where property_id is null
        return profileRepository.findAll().stream()
                .map(user -> Util.mapToRetrieveProfileResponse(user))
                .collect(Collectors.toList());
    }

    public void deleteAccount(Integer userId) {
        Optional<User> entity = userRepository.findById(userId);
        if (entity.isPresent()) {
            userRepository.deleteById(userId);
        } else {
            throw new EntityNotFoundException("Entity with id " + userId + " not found");
        }
    }

    public void updateProfile(Integer userId, MultipartFile[] images, UpdateProfileRequest request) throws IllegalAccessException {
        Profile user = profileRepository.findByProfileOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));
        Set<ID> ids = new HashSet<>();
        Util.multipartFileToIDList(userId, profileRepository, images, ids, imageService);

        // Update user details based on UpdateProfileRequest
        user.setIds(ids);
        Util.updateProfileFromRequest(user, request);
        // Save the updated user
        profileRepository.save(user);

    }

    public UpdateAccountResponse updateAccount(Integer userId, UpdateAccountRequest request) throws IllegalAccessException {
        // Find the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user details based on UpdateProfileRequest
        Map<String, Object> updatedFields = Util.updateAccountFromRequest(user, request);

        // Save the updated user
        userRepository.save(user);

        // Return the updated profile response
        return Util.mapToUpdateAccountResponse(updatedFields);
    }

    @Transactional
    public List<RetrieveAccountResponse> listAllAccounts() {
        // Retrieve all accounts and map them to RetrieveAccountResponse objects
        return userRepository.findAll().stream()
                .map(user -> Util.mapToRetrieveAccountResponse(user))
                .collect(Collectors.toList());
    }

    public void banAccount(Integer userId, Timestamp bannedTill) {
        // Retrieve the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Set the bannedTill timestamp
        user.setBannedTill(bannedTill);

        // Save the updated user back to the repository
        userRepository.save(user);
    }

    public List<IDImageResponse> getIdsByUserId(Integer userId) {
        Profile profile = profileRepository.findByProfileOwnerId(userId).orElseThrow(
                () -> new RuntimeException("Profile not found")
        );
        List<ID> idImageResponses = idRepository.findAllByProfileId(Math.toIntExact(profile.getId()));
        return idImageResponses.stream()
                .map(Util::convertFromIDImageToImageResponse) // Assuming ImageResponse has a constructor that takes a PropertyImage
                .collect(Collectors.toList());
    }

    public RetrieveProfileResponse retrieveProfile(Integer userId) {
        Profile profile = profileRepository.findByProfileOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));
        return new RetrieveProfileResponse(profile);
    }

    public RetrieveAccountResponse retrieveAccount(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return new RetrieveAccountResponse(user);
    }

    public void unbanAccount(Integer userId) {
        // Retrieve the user by userId
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Set the bannedTill timestamp
        user.setBannedTill(null);

        // Save the updated user back to the repository
        userRepository.save(user);
    }

    public IDImageResponse getIdByIdId(Integer userId, Integer idId) {
        ID id = idRepository.findById(idId).orElseThrow(() -> new RuntimeException("ID not found"));
        return convertFromIDImageToImageResponse(id);
    }

    public void deleteIdByIdId(Integer userId, Integer idId) {
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

    public void addIdsToProfile(MultipartFile[] images, Integer userId) {
        Set<ID> ids = new HashSet<>();
        Profile profile = this.profileRepository.findByProfileOwnerId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        Util.multipartFileToIDList(profile.getProfileOwner().getId(), profileRepository, images, ids, imageService);

        //set images property id to saved property
        try {
            ids.forEach(id -> id.setProfile(profile));
            this.idRepository.saveAll(ids);
        } catch (Exception e) {
            throw new RuntimeException("Could not save image: " + e);
        }
    }
    // Helper methods for mapping and updating

}
