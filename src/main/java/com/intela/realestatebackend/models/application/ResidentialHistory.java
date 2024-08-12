package com.intela.realestatebackend.models.application;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "residential_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResidentialHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private int years;
    private int months;
    private double weeklyRent;
    private String screeningQuestion;
    private String applicantResponse;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private UserProfile profile;

    @ManyToOne
    @JoinColumn(name = "reference_id")
    private Reference reference;
}
