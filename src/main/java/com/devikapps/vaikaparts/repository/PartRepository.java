package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartRepository extends JpaRepository<JPart, String> {}
