package com.capston_design.fkiller.itoms.service_desk.model;

import com.capston_design.fkiller.itoms.service_desk.model.common.BaseEntity;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name= "t_incident")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Incident extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    private LocalDateTime requestDT;
    private LocalDateTime acceptDT;
    private LocalDateTime endDT;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long createrById;
    private Long chargerById;

    private String creater;
    private String charger;
}
