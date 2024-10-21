package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intela.realestatebackend.models.archetypes.ApplicationStatus;
import com.intela.realestatebackend.models.profile.ID;
import com.intela.realestatebackend.models.property.Application;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Date;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(value = {"ids"}, allowGetters = true)
public class ApplicationRequest extends Application {

    @Override
    @JsonIgnore
    public Set<ID> getIds() {
        return super.getIds();
    }

    @JsonIgnore
    public ApplicationStatus getStatus() {
        return super.getStatus();
    }

    @JsonIgnore
    public Date getSubmittedDate() {
        return super.getSubmittedDate();
    }
}
