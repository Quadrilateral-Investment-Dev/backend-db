package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.property.Application;
import com.intela.realestatebackend.models.property.Bookmark;
import com.intela.realestatebackend.models.property.Property;
import com.intela.realestatebackend.models.property.PropertyImage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

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

    public PropertyResponse(Property property) {
        BeanUtils.copyProperties(property, this);
        init();
    }

    @JsonIgnore
    public List<PropertyImage> getPropertyImages() {
        return super.getPropertyImages();
    }

    @JsonIgnore
    public Set<Application> getApplications() {
        return super.getApplications();
    }

    @JsonIgnore
    public Set<Bookmark> getBookmarks() {
        return super.getBookmarks();
    }

    private void init() {
        this.userId = this.getUser() != null ? this.getUser().getId() : null;
        this.parentId = this.getParentListing() != null ? this.getParentListing().getId() : null;
    }
}
