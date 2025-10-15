package org.acme.evolv.entity;

import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity 
@Table(name = "ai_scenario_option")
public class AiScenarioOptionEntity extends PanacheEntityBase {
    @Id
    public UUID scenarioId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "scenario_id")
    public AiScenarioEntity scenario;

    @Column(name = "save_history",nullable = false)
    public boolean saveHistory = true;

    @Column(name = "allow_upload",nullable = false)
    public boolean allowUpload = true;
}
