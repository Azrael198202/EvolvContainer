package org.acme.evolv.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity 
@Table(name = "ai_scenario")
public class AiScenarioEntity extends PanacheEntityBase {
    @Id public UUID id;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(nullable = false, length = 60)
    public String type;

    @Column(name="icon_url") public String iconUrl;
    @Column(columnDefinition = "text[]") public String[] tags;
    @Column(columnDefinition = "text") public String description;
    @Column(name="prompt_template", columnDefinition = "text", nullable = false)
    public String promptTemplate;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "created_at")
    public OffsetDateTime createdAt;

    @Column(name = "updated_at")
    public OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public AiScenarioOptionEntity option;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<AiScenarioSampleEntity> samples;
}