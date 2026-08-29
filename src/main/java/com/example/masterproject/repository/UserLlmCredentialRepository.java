package com.example.masterproject.repository;

import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.entity.UserLlmCredential;
import com.example.masterproject.model.enums.LlmProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLlmCredentialRepository extends JpaRepository<UserLlmCredential, Long> {

    List<UserLlmCredential> findByUserOrderByProviderAsc(User user);

    Optional<UserLlmCredential> findByUserAndProvider(User user, LlmProvider provider);

    boolean existsByUserAndProvider(User user, LlmProvider provider);
}
