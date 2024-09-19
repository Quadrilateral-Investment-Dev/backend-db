package com.intela.realestatebackend.models.profile;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "contact_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContactDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    private String contactNumber;
    private String contactEmail;

    @OneToOne
    @JoinColumn(name = "profile_id")
    @Schema(hidden = true)
    @JsonBackReference("profile-contactDetails")
    private Profile profile;
}
