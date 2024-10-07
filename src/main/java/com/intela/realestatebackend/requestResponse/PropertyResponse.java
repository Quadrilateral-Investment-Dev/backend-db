package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.property.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyResponse extends Property {
    private Integer userId;
    private Integer parentId;
    @JsonIgnore
    private List<PropertyImage> propertyImages;
    @JsonIgnore
    private Set<Application> applications;
    @JsonIgnore
    private Set<Bookmark> bookmarks;

    public PropertyResponse(Property property) {
        BeanUtils.copyProperties(property, this);
        init();
    }

    private void init() {
        this.userId = this.getUser() != null ? this.getUser().getId() : null;
        this.parentId = this.getParentListing() != null ? this.getParentListing().getId() : null;
    }
}
