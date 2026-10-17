package com.example.indexreviewer.service.index.smi;

import com.example.indexreviewer.exception.InvalidReviewDataException;
import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewResult;
import com.example.indexreviewer.model.ValidationResult;
import com.example.indexreviewer.service.index.IndexReview;
import com.example.indexreviewer.service.review.FfmcapCalculator;
import com.example.indexreviewer.service.review.ReviewDataValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SmiReview implements IndexReview {

    private final SmiConfiguration configuration;
    private final FfmcapCalculator ffmcapCalculator;
    private final ReviewDataValidator reviewDataValidator;

    @Override
    public IndexType indexType() {
        return IndexType.SMI;
    }

    @Override
    public ReviewResult review(ReviewCommand command) {
        ValidationResult validationResult = new ValidationResult();

        validationResult.add(reviewDataValidator.validateDates(command));
        long members = command.composition().stream().distinct().count();
        if (members != configuration.numberOfComponents()) {
            validationResult.addError("Composition has %d securities but the SMI needs %d"
                .formatted(members, configuration.numberOfComponents()));
        }
        throwIfErrors(validationResult);

        FfmcapResult ffmcapResult = ffmcapCalculator.calculate(command);
        validationResult.addAll(ffmcapResult.validation());
        Map<Integer, BigDecimal> ffmcapValues = ffmcapResult.ffmcaps();

        if (ffmcapValues.size() < configuration.numberOfComponents()) {
            validationResult.addError("Only %d securities have a valid FFMCAP but the SMI needs %d"
                .formatted(ffmcapValues.size(), configuration.numberOfComponents()));
        }
        throwIfErrors(validationResult);

        throw new UnsupportedOperationException(
            "SMI review steps after the FFMCAP calculation are not implemented yet");
    }

    private static void throwIfErrors(ValidationResult validationResult) {
        if (validationResult.hasErrors()) {
            throw new InvalidReviewDataException(validationResult.getErrors());
        }
    }

}
