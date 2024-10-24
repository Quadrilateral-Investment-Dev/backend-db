package com.intela.realestatebackend.models.property;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PaymentCycle;
import com.intela.realestatebackend.models.archetypes.PropertyStatus;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import com.intela.realestatebackend.models.profile.ID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity(name = "properties")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Integer id;

    private String propertyOwnerName;
    @NotNull
    private String location;
    private String description;
    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentCycle paymentCycle;
    @Enumerated(EnumType.STRING)
    @NotNull
    private PropertyType propertyType;
    @Enumerated(EnumType.STRING)
    private PropertyStatus status = PropertyStatus.AVAILABLE;
    @NotNull
    private Long price;
    @Enumerated(EnumType.STRING)
    private BillType billType;
    private Timestamp availableFrom;
    private Timestamp availableTill;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "feature_id")
    @NotNull
    private Feature feature;

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "property",
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonManagedReference("property-propertyImages")
    private List<PropertyImage> propertyImages = new ArrayList<>();

    public void setPropertyImages(List<PropertyImage> propertyImages) {
        if (this.propertyImages == null) {
            this.propertyImages = propertyImages;
        } else {
            this.propertyImages.clear();
            this.propertyImages.addAll(propertyImages);
        }
    }

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "property",
            orphanRemoval = true
    )
    @JsonManagedReference("property-bookmarks")
    @Schema(hidden = true)
    private Set<Bookmark> bookmarks;

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "property",
            orphanRemoval = true
    )
    @JsonManagedReference("property-applications")
    @Schema(hidden = true)
    private Set<Application> applications;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    @Schema(hidden = true)
    @JsonBackReference("user-properties")
    private User user;

    // Self-referencing relationship for parent and child properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_listing", referencedColumnName = "id")
    @Schema(hidden = true)
    @JsonBackReference("property-plans")
    private Property parentListing; // Parent property (main property)

    @OneToMany(mappedBy = "parentListing", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("property-plans")
    private Set<Plan> plans = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdDate;

    @JsonSetter(nulls = Nulls.SKIP)  // Skip setting to null and retain the default value if the field is absent or null
    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public Integer getNumberOfRooms() {
        if (feature == null) {
            return 0;  // Or some other default value
        }
        return feature.getBedrooms() + feature.getLounges();
    }
}
