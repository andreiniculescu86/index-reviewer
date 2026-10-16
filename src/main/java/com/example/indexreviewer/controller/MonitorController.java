package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.swagger.MonitorSwaggerDocs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
@MonitorSwaggerDocs.Api
public class MonitorController {

    @GetMapping(path = "/reviews/{reviewId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
    @MonitorSwaggerDocs.GetReviewTrace
    public void getReviewTrace(@PathVariable String reviewId) {
    }

    @GetMapping(path = "/reviews/{reviewId}/securities/{securityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @MonitorSwaggerDocs.GetReviewSecurity
    public void getReviewSecurity(@PathVariable String reviewId, @PathVariable String securityId) {
    }
//
//    @GetMapping(path = "/reviews/{reviewId}/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
//    @MonitorSwaggerDocs.GetReviewManifest
//    public void getReviewManifest(@PathVariable String reviewId) {
//    }
//
//    @GetMapping(path = "/reviews/{reviewId}/inputs/{role}", produces = TEXT_CSV)
//    @MonitorSwaggerDocs.GetReviewInput
//    public void getReviewInput(@PathVariable String reviewId, @PathVariable String role) {
//    }
}
