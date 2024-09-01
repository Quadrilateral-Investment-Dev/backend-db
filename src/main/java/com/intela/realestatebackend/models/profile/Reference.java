package com.intela.realestatebackend.models.profile;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "user_references")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;
    private String relationship;
    private String contactNumber;
    private String contactEmail;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private CustomerInformation profile;

    @OneToMany(mappedBy = "reference")
    private Set<ResidentialHistory> residentialHistories;

    @OneToMany(mappedBy = "reference")
    private Set<EmploymentHistory> employmentHistories;
}