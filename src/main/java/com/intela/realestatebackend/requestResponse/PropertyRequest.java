package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.property.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.sql.Date;
import java.util.List;
import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@JsonIgnoreProperties(value = {"propertyImages"}, allowGetters = true, ignoreUnknown = true)
public class PropertyRequest extends Property {

    @Override
    @JsonIgnore
    public List<PropertyImage> getPropertyImages() {
        return super.getPropertyImages();
    }

    @JsonIgnore
    public Set<Plan> getPlans() {
        return super.getPlans();
    }

    @JsonIgnore
    public Date getCreatedDate() {
        return super.getCreatedDate();
    }

    @JsonIgnore
    public Property getParentListing() {
        return super.getParentListing();
    }

    @JsonIgnore
    public Set<Application> getApplications() {
        return super.getApplications();
    }

    @JsonIgnore
    public Set<Bookmark> getBookmarks() {
        return super.getBookmarks();
    }
}
