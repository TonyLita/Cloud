package com.example.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    @Id
    private Integer id;

    @Column(nullable = false, length = 20)
    private String label;
}
