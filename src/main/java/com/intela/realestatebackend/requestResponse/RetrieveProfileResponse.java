package com.intela.realestatebackend.requestResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intela.realestatebackend.models.profile.ID;
import com.intela.realestatebackend.models.profile.Profile;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class RetrieveProfileResponse extends Profile {
    private Integer userId;

    public RetrieveProfileResponse(Profile profile) {
        BeanUtils.copyProperties(profile, this);
        init();
    }

    @JsonIgnore
    public Set<ID> getIds() {
        return super.getIds();
    }

    private void init() {
        this.userId = this.getProfileOwner() != null ? this.getProfileOwner().getId() : null;
    }
}
