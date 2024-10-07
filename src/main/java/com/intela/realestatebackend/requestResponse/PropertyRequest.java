package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intela.realestatebackend.models.archetypes.PropertyStatus;
import com.intela.realestatebackend.models.property.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyRequest extends Property {
    @JsonIgnore
    private List<PropertyImage> propertyImages;
    @JsonIgnore
    private Set<Plan> plans;
    @JsonIgnore
    private Date createdDate;
    @JsonIgnore
    private Property parentListing;
    @JsonIgnore
    private Set<Application> applications;
    @JsonIgnore
    private Set<Bookmark> bookmarks;
}
