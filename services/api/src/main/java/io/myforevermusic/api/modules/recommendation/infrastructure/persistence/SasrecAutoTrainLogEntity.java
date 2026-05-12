package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sasrec_auto_train_log")
public class SasrecAutoTrainLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sasrec_auto_train_log_id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "trained_at", nullable = false)
    private Instant trainedAt;

    @Column(name = "event_count_at_train", nullable = false)
    private long eventCountAtTrain;

    @Column(name = "model_version", length = 200)
    private String modelVersion;

    @Column(name = "qualified", nullable = false)
    private boolean qualified;

    @Column(name = "promoted", nullable = false)
    private boolean promoted;

    @Column(name = "summary", length = 1000)
    private String summary;

    protected SasrecAutoTrainLogEntity() {}

    public SasrecAutoTrainLogEntity(SasrecAutoTrainLogStore.Draft draft) {
        this.userId = draft.userId();
        this.trainedAt = draft.trainedAt();
        this.eventCountAtTrain = draft.eventCountAtTrain();
        this.modelVersion = truncate(draft.modelVersion(), 200);
        this.qualified = draft.qualified();
        this.promoted = draft.promoted();
        this.summary = truncate(draft.summary(), 1000);
    }

    public SasrecAutoTrainLogStore.Entry toEntry() {
        return new SasrecAutoTrainLogStore.Entry(
            id,
            userId,
            trainedAt,
            eventCountAtTrain,
            modelVersion,
            qualified,
            promoted,
            summary
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
