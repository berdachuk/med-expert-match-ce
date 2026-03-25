package com.berdachuk.medexpertmatch.retrieval.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configurable weights for retrieval and routing scoring.
 */
@Getter
@Component
public class RetrievalScoringProperties {

    private static final double WEIGHT_SUM_TOLERANCE = 0.0001;

    private final double doctorVectorWeight;
    private final double doctorGraphWeight;
    private final double doctorHistoricalWeight;
    private final double facilityComplexityWeight;
    private final double facilityHistoricalWeight;
    private final double facilityCapacityWeight;
    private final double facilityGeographicWeight;
    private final double priorityUrgencyWeight;
    private final double priorityComplexityWeight;
    private final double priorityAvailabilityWeight;

    public RetrievalScoringProperties(
            @Value("${medexpertmatch.retrieval.scoring.doctor.vector-weight:0.4}") double doctorVectorWeight,
            @Value("${medexpertmatch.retrieval.scoring.doctor.graph-weight:0.3}") double doctorGraphWeight,
            @Value("${medexpertmatch.retrieval.scoring.doctor.historical-weight:0.3}") double doctorHistoricalWeight,
            @Value("${medexpertmatch.retrieval.scoring.facility.complexity-weight:0.3}") double facilityComplexityWeight,
            @Value("${medexpertmatch.retrieval.scoring.facility.historical-weight:0.3}") double facilityHistoricalWeight,
            @Value("${medexpertmatch.retrieval.scoring.facility.capacity-weight:0.2}") double facilityCapacityWeight,
            @Value("${medexpertmatch.retrieval.scoring.facility.geographic-weight:0.2}") double facilityGeographicWeight,
            @Value("${medexpertmatch.retrieval.scoring.priority.urgency-weight:0.5}") double priorityUrgencyWeight,
            @Value("${medexpertmatch.retrieval.scoring.priority.complexity-weight:0.3}") double priorityComplexityWeight,
            @Value("${medexpertmatch.retrieval.scoring.priority.availability-weight:0.2}") double priorityAvailabilityWeight) {
        this.doctorVectorWeight = doctorVectorWeight;
        this.doctorGraphWeight = doctorGraphWeight;
        this.doctorHistoricalWeight = doctorHistoricalWeight;
        this.facilityComplexityWeight = facilityComplexityWeight;
        this.facilityHistoricalWeight = facilityHistoricalWeight;
        this.facilityCapacityWeight = facilityCapacityWeight;
        this.facilityGeographicWeight = facilityGeographicWeight;
        this.priorityUrgencyWeight = priorityUrgencyWeight;
        this.priorityComplexityWeight = priorityComplexityWeight;
        this.priorityAvailabilityWeight = priorityAvailabilityWeight;

        validateWeightGroup("doctor scoring", doctorVectorWeight, doctorGraphWeight, doctorHistoricalWeight);
        validateWeightGroup("facility scoring",
                facilityComplexityWeight, facilityHistoricalWeight, facilityCapacityWeight, facilityGeographicWeight);
        validateWeightGroup("priority scoring", priorityUrgencyWeight, priorityComplexityWeight, priorityAvailabilityWeight);
    }

    private void validateWeightGroup(String groupName, double... weights) {
        double sum = 0.0;
        for (double weight : weights) {
            if (weight < 0.0 || weight > 1.0) {
                throw new IllegalArgumentException(groupName + " weights must be between 0 and 1");
            }
            sum += weight;
        }

        if (Math.abs(sum - 1.0) > WEIGHT_SUM_TOLERANCE) {
            throw new IllegalArgumentException(groupName + " weights must sum to 1.0");
        }
    }
}
