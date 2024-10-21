package com.intela.realestatebackend.models.property;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.intela.realestatebackend.models.UploadedFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity(name = "property_images")
public class PropertyImage extends UploadedFile {
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "property_id")
    @ToString.Exclude
    @Schema(hidden = true)
    @JsonBackReference("property-propertyImages")
    private Property property;
}
