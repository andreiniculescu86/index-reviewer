package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.swagger.ReviewSwaggerDocs;
import com.example.indexreviewer.controller.mapper.ReviewCommandMapper;
import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.exception.InvalidCsvFileException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.repository.FileReader;
import com.example.indexreviewer.repository.FileReaderFactory;
import com.example.indexreviewer.repository.csv.CsvFormat;
import com.example.indexreviewer.service.ReviewService;
import com.example.indexreviewer.validation.CsvFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
@ReviewSwaggerDocs.Api
public class ReviewController {

    @Qualifier("csvFileReaderFactory")
    private final FileReaderFactory fileReaderFactory;
    private final ReviewCommandMapper reviewMapper;
    private final ReviewService reviewService;

    @PostMapping(path = "/reviews", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ReviewSwaggerDocs.RunReviewWithUpload
    public void runReviewWithUpload(
        @Valid @RequestPart(name = "configRequest", required = false) ConfigRequest configRequest,
        @CsvFile @RequestPart("universe") MultipartFile universe,
        @CsvFile @RequestPart("securityData") MultipartFile securityData,
        @CsvFile @RequestPart("composition") MultipartFile composition) throws IOException {

        var reviewRequest = configRequest != null ? configRequest : new ConfigRequest();
        var fileReader = fileReaderFactory.create(new CsvFormat(reviewRequest.getCsvSeparator()));

        List<Universe> universeRows = read(fileReader, universe, Universe.class);
        List<SecurityData> secData = read(fileReader, securityData, SecurityData.class);
        List<Composition> compositions = read(fileReader, composition, Composition.class);

        log.info("Review requested: {}, universe rows: {}, security data rows: {}, composition rows: {}",
            reviewRequest, universeRows.size(), secData.size(), compositions.size());

        var reviewCommand = reviewMapper.toServiceModel(reviewRequest, universeRows, secData, compositions);
        reviewService.runReview(reviewCommand);
    }

    //rethrows exception in order to add the fine name with the error
    private static <T> List<T> read(FileReader fileReader, MultipartFile file, Class<T> type) throws IOException {
        try {
            return fileReader.read(file.getBytes(), type);
        } catch (InvalidCsvFileException exception) {
            throw new InvalidCsvFileException(file.getName(), exception.getMessage(), exception);
        }
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
