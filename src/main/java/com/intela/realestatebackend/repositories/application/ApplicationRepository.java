package com.intela.realestatebackend.repositories.application;

import com.intela.realestatebackend.models.User;
import com.intela.realestatebackend.models.property.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findAllByUser(User user);

    List<Application> findAllByPropertyId(Integer propertyId);
}
