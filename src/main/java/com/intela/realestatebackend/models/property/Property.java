package com.intela.realestatebackend.models.property;

import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.archetypes.BillType;
import com.intela.realestatebackend.models.archetypes.PropertyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private Integer id;

    private String propertyOwnerName;
    private String location;
    private String description;
    private Integer numberOfRooms;
    private PropertyType propertyType;
    private String status;
    private Long price;
    private BillType billType;
    private Timestamp availableFrom;
    private Timestamp availableTill;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "feature_id")
    private Feature feature;

    @OneToMany(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            mappedBy = "property"
    )
    @ToString.Exclude
    private List<PropertyImage> propertyImages = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    private User user;
}