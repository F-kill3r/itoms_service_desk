package com.capston_design.fkiller.itoms.service_desk.model;

import com.capston_design.fkiller.itoms.service_desk.model.common.BaseEntity;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Priority;
import com.capston_design.fkiller.itoms.service_desk.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incident extends BaseEntity {

    @Id
    private UUID id;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    private String title;
    private String content;

    private LocalDateTime requestDT;
    private LocalDateTime acceptDT;
    private LocalDateTime endDT;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private UUID ticketByID;
    private UUID createrById;
    private UUID chargerById;

    private String creater;
    private String charger;
}

