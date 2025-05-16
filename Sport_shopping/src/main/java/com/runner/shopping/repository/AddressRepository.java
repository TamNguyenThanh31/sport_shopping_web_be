package com.runner.shopping.repository;

import com.runner.shopping.entity.Addresses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Addresses, Long> {

    Optional<Addresses> findByIdAndUserId(Long id, Long userId);
}
