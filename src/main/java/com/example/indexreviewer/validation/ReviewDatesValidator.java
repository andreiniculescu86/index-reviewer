package com.example.indexreviewer.validation;

import com.example.indexreviewer.dto.request.ConfigRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReviewDatesValidator implements ConstraintValidator<ValidReviewDates, ConfigRequest> {

    @Override
    public boolean isValid(ConfigRequest request, ConstraintValidatorContext context) {
        return request == null || request.getCutoffDate() == null || request.getReviewDate() == null
            || !request.getCutoffDate().isAfter(request.getReviewDate());
    }
}
