package com.example.indexreviewer.service.index;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.SelectionResult;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.repository.ReviewAuditRepository;
import com.example.indexreviewer.repository.ReviewTraceRepository;
import com.example.indexreviewer.service.review.ComponentSelectorStep;
import com.example.indexreviewer.service.review.FfmcapCalculatorStep;
import com.example.indexreviewer.service.review.ReviewDataValidator;
import com.example.indexreviewer.service.review.WeightCalculatorStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reviews every configured index that is ranked by FFMCAP, selected with a buffer and weighted by FFMCAP with a
 * cap, such as the SMI. The validators and steps only return their results; this class stores the findings and trace
 * records of every validation and step as soon as it is done. A stage that finds errors ends the review, and the trace
 * keeps everything calculated until then, so that a failed review can be traced as well. The audit record is stored at
 * the end, with status ERROR when a step throws. The report is built from the step results, not read back from the
 * repositories.
 */
@Slf4j
@Component
public class RankedIndexReviewer implements IndexReviewer {

    private final IndexesConfiguration configuration;
    private final ReviewDataValidator reviewDataValidator;
    private final FfmcapCalculatorStep ffmcapCalculatorStep;
    private final ComponentSelectorStep componentSelectorStep;
    private final WeightCalculatorStep weightCalculatorStep;
    private final ReviewAuditRepository auditRepository;
    private final ReviewTraceRepository traceRepository;
    private final String appVersion;

    public RankedIndexReviewer(IndexesConfiguration configuration, ReviewDataValidator reviewDataValidator,
                               FfmcapCalculatorStep ffmcapCalculatorStep, ComponentSelectorStep componentSelectorStep,
                               WeightCalculatorStep weightCalculatorStep, ReviewAuditRepository auditRepository,
                               ReviewTraceRepository traceRepository,
                               @Value("${spring.application.version}") String appVersion) {
        this.configuration = configuration;
        this.reviewDataValidator = reviewDataValidator;
        this.ffmcapCalculatorStep = ffmcapCalculatorStep;
        this.componentSelectorStep = componentSelectorStep;
        this.weightCalculatorStep = weightCalculatorStep;
        this.auditRepository = auditRepository;
        this.traceRepository = traceRepository;
        this.appVersion = appVersion;
    }

    @Override
    public Set<IndexType> indexTypes() {
        return configuration.indices().keySet();
    }

    @Override
    public ReviewReport review(ReviewCommand command) {
        var reviewId = UUID.randomUUID();
        var executedAt = Instant.now();

        log.info("Starting review {} of {} for {} on {}", reviewId, command.getIndexType(), command.getCutoffDate(),
            command.getReviewDate());
        ReviewReport report;
        try {
            report = runSteps(reviewId, command, configuration.findRules(command.getIndexType()));
            log.info("Review {} finalised with status {}", reviewId, report.getStatus());
        } catch (RuntimeException | Error exception) {
            log.warn("Review {} of {} stopped on an unexpected error; {}", reviewId, command.getIndexType(),
                exception.getMessage());
            saveAuditError(reviewId, command, executedAt);
            throw exception;
        }
        saveAudit(reviewId, command, executedAt, report);
        return report;
    }

    // Each stage method runs one validation or step and stores what it returned in the review trace.
    private ReviewReport runSteps(UUID reviewId, ReviewCommand command, IndexRules rules) {
        var overallStatus = ReviewStatus.of(validateComposition(reviewId, command, rules));
        if (overallStatus == ReviewStatus.FAILED) {
            return ReviewReport.failed(reviewId);
        }

        FfmcapResult ffmcap = calculateFfmcap(reviewId, command);
        var candidatesValidations = validateCandidates(reviewId, command, ffmcap.getCandidates(), rules);
        overallStatus = overallStatus
            .getMostSevere(ReviewStatus.of(ffmcap.getFindings()))
            .getMostSevere(ReviewStatus.of(candidatesValidations));
        if (overallStatus == ReviewStatus.FAILED) {
            return ReviewReport.failed(reviewId);
        }

        var selectedCandidates = selectTopCandidates(reviewId, ffmcap.getCandidates(), command.getComposition(), rules);
        overallStatus = overallStatus.getMostSevere(ReviewStatus.of(selectedCandidates.getFindings()));
        if (overallStatus == ReviewStatus.FAILED) {
            return ReviewReport.failed(reviewId);
        }

        WeightResult weights = calculateWeights(reviewId, selectedCandidates.getComponents(), rules);
        overallStatus = overallStatus.getMostSevere(ReviewStatus.of(weights.getFindings()));
        if (overallStatus == ReviewStatus.FAILED) {
            return ReviewReport.failed(reviewId);
        }

        return ReviewReport.completed(reviewId, overallStatus, weights.getConstituents(),
            selectedCandidates.getComponents(), command.getComposition());
    }

    private List<Finding> validateComposition(UUID reviewId, ReviewCommand command, IndexRules rules) {
        List<Finding> findings = reviewDataValidator.validateComposition(command, rules);
        traceRepository.appendFindings(reviewId, findings);
        return findings;
    }

    private FfmcapResult calculateFfmcap(UUID reviewId, ReviewCommand command) {
        var ffmcap = ffmcapCalculatorStep.calculate(command);
        traceRepository.appendFfmcap(reviewId, ffmcap.getTraces());
        traceRepository.appendFindings(reviewId, ffmcap.getFindings());
        return ffmcap;
    }

    private List<Finding> validateCandidates(UUID reviewId, ReviewCommand command, List<Candidate> candidates,
                                             IndexRules rules) {
        var findings = reviewDataValidator.validateCandidates(command, candidates, rules);
        traceRepository.appendFindings(reviewId, findings);
        return findings;
    }

    private SelectionResult selectTopCandidates(UUID reviewId, List<Candidate> candidates, List<Integer> composition,
                                                IndexRules rules) {
        var selection = componentSelectorStep.selectTopCandidates(candidates, composition, rules);
        traceRepository.appendSelection(reviewId, selection.getTraces());
        traceRepository.appendFindings(reviewId, selection.getFindings());
        return selection;
    }

    private WeightResult calculateWeights(UUID reviewId, List<Candidate> components, IndexRules rules) {
        var weights = weightCalculatorStep.calculateWeights(components, rules.getCapPercentage());
        traceRepository.appendWeights(reviewId, weights.getTraces());
        traceRepository.appendFindings(reviewId, weights.getFindings());
        return weights;
    }

    private void saveAudit(UUID reviewId, ReviewCommand command, Instant executedAt, ReviewReport report) {
        var composition = report.getConstituents().stream().map(Constituent::getSecurityId).toList();
        auditRepository.save(ReviewAudit.builder()
            .reviewId(reviewId)
            .indexType(command.getIndexType())
            .cutoffDate(command.getCutoffDate())
            .reviewDate(command.getReviewDate())
            .status(report.getStatus())
            .executedAt(executedAt)
            .appVersion(appVersion)
            .rules(configuration.findRules(command.getIndexType()))
            .composition(composition)
            .build());
    }

    private void saveAuditError(UUID reviewId, ReviewCommand command, Instant executedAt) {
        auditRepository.save(ReviewAudit.builder()
            .reviewId(reviewId)
            .indexType(command.getIndexType())
            .cutoffDate(command.getCutoffDate())
            .reviewDate(command.getReviewDate())
            .status(ReviewStatus.ERROR)
            .executedAt(executedAt)
            .appVersion(appVersion)
            .rules(configuration.findRules(command.getIndexType()))
            .composition(List.of())
            .build());
    }
}
