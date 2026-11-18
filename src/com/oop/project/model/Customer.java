package com.oop.project.model;

import java.time.LocalDateTime;

import com.oop.project.exception.DomainException;

public class Customer {
    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static String validateName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Customer Name must not be empty");
        }

        fullName = fullName.trim();
        return fullName;
    }

    private static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new DomainException("Customer Phone must not be empty");
        }

        phone = phone.trim();

        if (!phone.matches("\\d+")) {
            throw new DomainException("Phone Number only contains numbers");
        }

        if (!phone.matches("\\d{10}")) {
            throw new DomainException("Phone Number must contain exactly 10 digits");
        }

        return phone;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public Customer(int id, String fullName, String phone, String email, String address, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = validateName(fullName);
        this.phone = validatePhone(phone);
        this.email = normalizeOptional(email);
        this.address = normalizeOptional(address);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Customer(String fullName, String phone, String email, String address) {
        this(0, fullName, phone, email, address, null, null);
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
