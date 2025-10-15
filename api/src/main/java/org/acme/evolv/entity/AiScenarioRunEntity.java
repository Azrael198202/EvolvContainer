package org.acme.evolv.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity 
@Table(name = "ai_scenario_run")
public class AiScenarioRunEntity extends PanacheEntityBase {
    @Id public UUID id;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "scenario_id", nullable = false)
    public AiScenarioEntity scenario;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    
    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "file_name", nullable = false)
    public String fileName;

    @Column(name = "model_used", nullable = false)
    public String modelUsed;

    @Column(name = "result_markdown", columnDefinition = "text") 
    public String resultMarkdown;

    @Column(name = "prompt_used", columnDefinition = "text")
    public String promptUsed;

    @Column(nullable = false) public String status = "done";

    @Column(name = "error_message", columnDefinition = "text") 
    public String errorMessage;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}