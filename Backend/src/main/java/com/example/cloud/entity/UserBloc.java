package com.example.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "userbloc")
@Getter
@Setter
@NoArgsConstructor
public class UserBloc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "blocked_at", updatable = false)
    private LocalDateTime blockedAt;

    private String reason;

    public UserBloc(User user, String reason) {
        this.user = user;
        this.reason = reason;
    }
}
