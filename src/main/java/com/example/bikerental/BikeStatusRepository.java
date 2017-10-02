package com.example.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BikeStatusRepository extends JpaRepository<BikeStatus, String> {

}
