package com.example.cloud.repository;

import com.example.cloud.entity.UserBloc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBlocRepository extends JpaRepository<UserBloc, Long> {
    Optional<UserBloc> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
