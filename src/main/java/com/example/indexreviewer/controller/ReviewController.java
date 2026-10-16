package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.swagger.ReviewSwaggerDocs;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.dto.request.IndexType;
import com.example.indexreviewer.validation.CsvFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
@ReviewSwaggerDocs.Api
public class ReviewController {

    @PostMapping(path = "/reviews", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ReviewSwaggerDocs.RunReviewWithUpload
    public void runReviewWithUpload(
        @Valid @RequestPart(name = "configRequest", required = false) ConfigRequest configRequest,
        @CsvFile @RequestPart("universe") MultipartFile universe,
        @CsvFile @RequestPart("securityData") MultipartFile securityData,
        @CsvFile @RequestPart("composition") MultipartFile composition) {

        ConfigRequest reviewRequest = configRequest != null ? configRequest : new ConfigRequest();
        log.info("Review requested: {}", reviewRequest);
    }

    @GetMapping(path = "/reviews", produces = APPLICATION_JSON_VALUE)
    @ReviewSwaggerDocs.ListReviews
    public void listReviews(@RequestParam(required = false) IndexType indexType,
                            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate cutoffDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate reviewDate) {
    }

    @GetMapping(path = "/reviews/{reviewId}", produces = APPLICATION_JSON_VALUE)
    @ReviewSwaggerDocs.GetReview
    public void getReview(@PathVariable String reviewId) {
    }
}
