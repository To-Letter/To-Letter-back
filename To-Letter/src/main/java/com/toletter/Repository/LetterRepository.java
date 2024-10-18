package com.toletter.Repository;

import com.toletter.Entity.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    Optional<Letter> findById(Long id);
}
