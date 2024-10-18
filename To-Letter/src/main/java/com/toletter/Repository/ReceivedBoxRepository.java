package com.toletter.Repository;

import com.toletter.Entity.ReceivedBox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReceivedBoxRepository extends JpaRepository<ReceivedBox, Long> {

    Optional<ReceivedBox> findByLetterId(Long letter);

}
