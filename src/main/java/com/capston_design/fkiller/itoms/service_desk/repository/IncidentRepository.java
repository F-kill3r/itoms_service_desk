package com.capston_design.fkiller.itoms.service_desk.repository;

import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

}
