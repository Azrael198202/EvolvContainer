package org.acme.evolv.entity;

import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity 
@Table(name = "ai_scenario_sample")
public class AiScenarioSampleEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    public AiScenarioEntity scenario;

    @Column(nullable = false)
    public Integer position;

    @Column(name = "sample_q", columnDefinition = "text", nullable = false)
    public String sampleQ;

    @Column(name = "sample_a", columnDefinition = "text")
    public String sampleA;
}
