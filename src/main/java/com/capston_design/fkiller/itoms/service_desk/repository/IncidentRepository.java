package com.capston_design.fkiller.itoms.service_desk.repository;

import com.capston_design.fkiller.itoms.service_desk.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

}
