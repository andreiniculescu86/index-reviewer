package com.example.indexreviewer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public class CsvFileValidator implements ConstraintValidator<CsvFile, MultipartFile> {

    private static final String CSV_EXTENSION = ".csv";

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(CSV_EXTENSION);
    }
}
