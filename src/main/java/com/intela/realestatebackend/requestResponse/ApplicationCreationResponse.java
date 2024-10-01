package com.intela.realestatebackend.requestResponse;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

@Data
public class ApplicationCreationResponse {
    private Long id;
    private Integer userId;
    private Integer propertyId;
}
