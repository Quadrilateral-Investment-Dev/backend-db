package com.intela.realestatebackend.models.property;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.archetypes.ApplicationStatus;
import com.intela.realestatebackend.models.profile.Profile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "property_applications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Application extends Profile {
    @ManyToOne
    @JoinColumn(name = "property_id")
    @Schema(hidden = true)
    @JsonBackReference("property-applications")
    private Property property;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @Schema(hidden = true)
    @JsonBackReference("user-applications")
    private User user;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.UNREAD;
    private String message;
    @CreationTimestamp
    @Column(updatable = false)
    private Date submittedDate;

    private Timestamp earliestMoveInFrom;
    private Timestamp latestMoveInBy;
    private Timestamp earliestCheckoutOn;
    private Timestamp latestCheckoutBy;

    @JsonSetter(nulls = Nulls.SKIP)  // Skip setting to null and retain the default value if the field is absent or null
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

}
