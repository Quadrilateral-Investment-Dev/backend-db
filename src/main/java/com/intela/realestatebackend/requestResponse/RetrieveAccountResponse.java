package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.ProfileImage;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.profile.Profile;
import com.intela.realestatebackend.models.property.Application;
import com.intela.realestatebackend.models.property.Bookmark;
import com.intela.realestatebackend.models.property.Property;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetrieveAccountResponse extends User {
    public RetrieveAccountResponse(User user) {
        BeanUtils.copyProperties(user, this);
    }

    @JsonIgnore
    public List<Property> getProperties() {
        return super.getProperties();
    }

    @JsonIgnore
    public List<Bookmark> getBookmarks() {
        return super.getBookmarks();
    }

    @JsonIgnore
    public Profile getProfile() {
        return super.getProfile();
    }

    @JsonIgnore
    public Set<Application> getApplications() {
        return super.getApplications();
    }

    @JsonIgnore
    public ProfileImage getProfileImage() {
        return super.getProfileImage();
    }
}
