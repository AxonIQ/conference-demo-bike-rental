package com.example.bikerental;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class BikeStatus {

    @Id
    private String id;
    private String location;
    private boolean rented;
    private String renter;

    public BikeStatus(String id, String location) {
        this.id = id;
        this.location = location;
        this.rented = false;
    }

    public BikeStatus() {
    }

    public String getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public boolean isRented() {
        return rented;
    }

    public String getRenter() {
        return renter;
    }

    public void markRented(String renter) {
        this.rented = true;
        this.renter = renter;
    }

    public void markReturned(String location) {
        this.rented = false;
        this.renter = null;
        this.location = location;
    }
}
