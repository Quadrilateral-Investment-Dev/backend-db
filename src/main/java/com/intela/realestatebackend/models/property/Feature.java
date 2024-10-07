package com.intela.realestatebackend.models.property;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "features")
@EqualsAndHashCode(exclude = "id")
public class Feature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Integer id;
    private Integer bathrooms = 0;
    private Integer bedrooms = 0;
    private Integer lounges = 0;
    private Integer parking = 0;
}
