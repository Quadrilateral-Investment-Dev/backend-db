package com.intela.realestatebackend.services;

import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.archetypes.ApplicationStatus;
import com.intela.realestatebackend.models.profile.ID;
import com.intela.realestatebackend.models.property.Application;
import com.intela.realestatebackend.models.property.Bookmark;
import com.intela.realestatebackend.models.property.Property;
import com.intela.realestatebackend.repositories.BookmarkRepository;
import com.intela.realestatebackend.repositories.ProfileRepository;
import com.intela.realestatebackend.repositories.PropertyRepository;
import com.intela.realestatebackend.repositories.UserRepository;
import com.intela.realestatebackend.repositories.application.ApplicationRepository;
import com.intela.realestatebackend.repositories.application.IDRepository;
import com.intela.realestatebackend.requestResponse.*;
import com.intela.realestatebackend.util.Util;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intela.realestatebackend.util.Util.*;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ProfileRepository profileRepository;
    private final JwtService jwtService;
    private final ApplicationRepository applicationRepository;
    private final UploadedFileService imageService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private IDRepository idRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public List<BookmarkResponse> fetchAllBookmarksByUserId(HttpServletRequest servletRequest, Pageable pageRequest) {
        User loggedUser = getUserByToken(servletRequest, jwtService, this.userRepository);
        List<Bookmark> bookmarks = this.bookmarkRepository.findAllByUserId(loggedUser.getId(), pageRequest);
        List<BookmarkResponse> bookmarkResponses = new ArrayList<>();

        bookmarks.forEach(bookmark -> {
            bookmarkResponses.add(new BookmarkResponse(bookmark));
        });

        return bookmarkResponses;
    }

    public BookmarkResponse fetchBookmarkById(Integer bookmarkId, HttpServletRequest servletRequest) {
        User loggedUser = getUserByToken(servletRequest, jwtService, this.userRepository);
        Bookmark bookmark = this.bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Bookmark not found"));
        if (bookmark.getUser().getId() == loggedUser.getId())
            return new BookmarkResponse(bookmark);
        else
            return null;
    }

    public void addBookmark(Integer propertyId, HttpServletRequest servletRequest) {

        if (this.bookmarkRepository.findByPropertyId(propertyId) != null) {
            throw new RuntimeException("Property already exists in your bookmarks");
        }

        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Property property = this.propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .property(property)
                .build();
        this.bookmarkRepository.save(bookmark);
    }

    public void removeBookmark(Integer bookmarkId, HttpServletRequest servletRequest) {
        // Retrieve the user by token from the request
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Find the bookmark by its ID
        Bookmark bookmark = this.bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Bookmark not found"));

        // Check if the bookmark belongs to the user
        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to remove this bookmark");
        }

        // Remove the bookmark from the repository
        this.bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public ApplicationCreationResponse createApplication(Integer propertyId, HttpServletRequest servletRequest, ApplicationRequest request, MultipartFile[] images) {
        // Retrieve the user by token from the request
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);
        Set<ID> ids = new HashSet<>();
        if (request.getId() != null) {
            request.setId(null);
        }
        // Find the property by its ID
        Property property = this.propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        // Create a new CustomerInformation entity using the request data
        request.setStatus(ApplicationStatus.UNREAD);
        request.setSubmittedDate(new Date(System.currentTimeMillis()));
        request.setUser(user);
        request.setProperty(property);
        // Save the CustomerInformation entity
        this.applicationRepository.save(request);
        Util.multipartFileToIDList(request.getId(), applicationRepository, images, ids, imageService);
        request.setIds(ids);

        ApplicationCreationResponse applicationCreationResponse = new ApplicationCreationResponse();
        applicationCreationResponse.setId(request.getId());
        applicationCreationResponse.setPropertyId(request.getProperty().getId());
        applicationCreationResponse.setUserId(request.getUser().getId());

        return applicationCreationResponse;
    }

    public List<ApplicationResponse> getAllApplications(HttpServletRequest servletRequest) {
        // Retrieve the user by token from the request
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Fetch all CustomerInformation entities for the user where propertyId is not null
        List<Application> applications = this.applicationRepository.findAllByUser(user);

        // Filter out entries where property is null
        List<ApplicationResponse> applicationResponses = applications.stream()
                .filter(info -> info.getProperty() != null)
                .map(ApplicationResponse::new)
                .collect(Collectors.toList());

        return applicationResponses;
    }

    public ApplicationResponse getApplication(Integer applicationId, HttpServletRequest servletRequest) {
        // Retrieve the user by token from the request
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Find the CustomerInformation by its ID
        Application application = this.applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        // Check if the CustomerInformation belongs to the user
        if (!application.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to access this application");
        }

        ApplicationResponse response = mapApplicationToApplicationResponse(application);

        // Return the ApplicationResponse
        return response;
    }

    public void withdrawApplication(Integer applicationId, HttpServletRequest servletRequest) {
        // Retrieve the user by token from the request
        User user = getUserByToken(servletRequest, jwtService, this.userRepository);

        // Find the CustomerInformation by its ID
        Application application = this.applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        // Check if the CustomerInformation belongs to the user
        if (!application.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to withdraw this application");
        }

        // Delete the CustomerInformation (withdraw the application)
        this.applicationRepository.delete(application);
    }

    public List<IDImageResponse> getIdsByApplicationId(Integer applicationId, HttpServletRequest servletRequest) {
        List<ID> idImageResponses = idRepository.findAllByProfileId(applicationId);
        return idImageResponses.stream()
                .map(Util::convertFromIDImageToImageResponse) // Assuming ImageResponse has a constructor that takes a PropertyImage
                .collect(Collectors.toList());
    }

    public IDImageResponse getIdByIdId(Integer applicationId, Integer idId, HttpServletRequest servletRequest) {
        ID id = idRepository.findById(idId).orElseThrow(() -> new RuntimeException("ID not found"));
        return convertFromIDImageToImageResponse(id);
    }

    public void deleteIdByIdId(Integer applicationId, Integer idId, HttpServletRequest servletRequest) {
        idRepository.deleteById(idId);
    }

    public void addIdsToApplication(MultipartFile[] images, Integer applicationId) {
        Set<ID> ids = new HashSet<>();
        Application application = this.applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        multipartFileToIDList(Long.valueOf(applicationId), applicationRepository, images, ids, imageService);

        //set images property id to saved property
        try {
            ids.forEach(id -> id.setProfile(application));
            this.idRepository.saveAll(ids);
        } catch (Exception e) {
            throw new RuntimeException("Could not save image: " + e);
        }
    }
}
