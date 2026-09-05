package com.shopsphere.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
   @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Column(nullable = false, length = 100)
    private String name;

   @Column(nullable = false, unique = true, length = 150)
    private String email;

   @Column(nullable = false)
    private String password;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private Role role;

   public User() {}

    public User(Long id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}