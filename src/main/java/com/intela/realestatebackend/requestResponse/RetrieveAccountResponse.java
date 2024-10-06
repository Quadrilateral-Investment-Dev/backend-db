package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.profile.Profile;
import com.intela.realestatebackend.models.property.Application;
import com.intela.realestatebackend.models.property.Bookmark;
import com.intela.realestatebackend.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetrieveAccountResponse extends User {
    @JsonIgnore
    private List<Property> properties = new ArrayList<>();
    @JsonIgnore
    private List<Bookmark> bookmarks = new ArrayList<>();
    @JsonIgnore
    private Profile profile;
    @JsonIgnore
    private Set<Application> applications;
    
    public RetrieveAccountResponse(User user) {
        BeanUtils.copyProperties(user, this);
    }
}
