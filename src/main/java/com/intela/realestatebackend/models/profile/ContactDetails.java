package com.intela.realestatebackend.models.profile;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contactNumber;
    private String contactEmail;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private CustomerInformation profile;
}