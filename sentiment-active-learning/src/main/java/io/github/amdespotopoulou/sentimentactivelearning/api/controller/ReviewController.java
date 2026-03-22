package io.github.amdespotopoulou.sentimentactivelearning.api.controller;

import io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.LabelRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.PredictionFeedbackRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.ReviewRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ClassifyResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ReviewSampleResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.github.amdespotopoulou.sentimentactivelearning.commons.mapper.ReviewSampleMapper;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao;
import io.github.amdespotopoulou.sentimentactivelearning.service.core.ActiveLearner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for review sample submission, retrieval, and labelling.
 *
 * <p>Exposes the following endpoints:
 * <ul>
 *   <li>{@code POST   /api/reviews}               — submit a review for classification</li>
 *   <li>{@code GET    /api/reviews}               — list all review samples</li>
 *   <li>{@code GET    /api/reviews/labelled}       — list labelled samples only</li>
 *   <li>{@code GET    /api/reviews/unlabelled}     — list unlabelled samples only</li>
 *   <li>{@code GET    /api/reviews/{id}}           — retrieve a specific sample</li>
 *   <li>{@code PATCH  /api/reviews/{id}/label}     — manually assign a label</li>
 *   <li>{@code POST   /api/reviews/{id}/feedback}  — submit a prediction correction</li>
 * </ul>
 *
 * <p>All error responses follow the {@link ApiErrorResponse} envelope.
 * Each endpoint documents every possible {@link ErrorCode} it can produce
 * using named {@code @ExampleObject} entries so Swagger UI shows the exact
 * JSON shape for each failure scenario.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Reviews",
        description = "Submit movie reviews for sentiment classification, " +
                "manage labelled samples, and provide human feedback " +
                "to the active-learning pipeline."
)
public class ReviewController {

    /** Active-learning orchestration service. */
    private final ActiveLearner activeLearner;

    /** DAO for direct retrieval queries not requiring orchestration. */
    private final ReviewSampleDao reviewSampleDao;

    /** Mapper for converting entities to response DTOs. */
    private final ReviewSampleMapper reviewSampleMapper;

    // -------------------------------------------------------------------------
    // POST /api/reviews
    // -------------------------------------------------------------------------

    /**
     * Submits a new movie review for sentiment classification by the
     * active-learning pipeline.
     *
     * @param request the review submission body; must not be {@code null}
     * @return a {@link ClassifyResponse} with the classification result
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit a review for classification",
            description = "Persists the review, classifies it using the trained Naive Bayes " +
                    "model, and consults the Claude AI oracle if the model is uncertain. " +
                    "Returns the assigned sentiment label, confidence score, and label source."
    )
    @ApiResponse(responseCode = "201",
            description = "Review classified successfully.")
    @ApiResponse(responseCode = "400",
            description = "Request validation failed.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "VALIDATION_ERROR — blank review text",
                                    value = """
                                            {
                                              "httpStatus"     : 400,
                                              "httpStatusText" : "Bad Request",
                                              "errorCode"      : "VALIDATION_ERROR",
                                              "message"        : "Review text must not be blank"
                                            }"""),
                            @ExampleObject(
                                    name = "VALIDATION_ERROR — review text too long",
                                    value = """
                                            {
                                              "httpStatus"     : 400,
                                              "httpStatusText" : "Bad Request",
                                              "errorCode"      : "VALIDATION_ERROR",
                                              "message"        : "Review text must not exceed 5000 characters"
                                            }""")
                    }))
    @ApiResponse(responseCode = "409",
            description = "Business rule conflict.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "REVIEW_ALREADY_EXISTS — duplicate review",
                                    value = """
                                            {
                                              "httpStatus"     : 409,
                                              "httpStatusText" : "Conflict",
                                              "errorCode"      : "REVIEW_ALREADY_EXISTS",
                                              "message"        : "A review sample with the same text already exists."
                                            }"""),
                            @ExampleObject(
                                    name = "MODEL_NOT_TRAINED — no trained model available",
                                    value = """
                                            {
                                              "httpStatus"     : 409,
                                              "httpStatusText" : "Conflict",
                                              "errorCode"      : "MODEL_NOT_TRAINED",
                                              "message"        : "The model has not been trained yet. Please submit labelled samples and trigger a training run first."
                                            }""")
                    }))
    @ApiResponse(responseCode = "502",
            description = "Claude AI oracle integration failure.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "CLAUDE_API_UNAVAILABLE — network or timeout error",
                                    value = """
                                            {
                                              "httpStatus"     : 502,
                                              "httpStatusText" : "Bad Gateway",
                                              "errorCode"      : "CLAUDE_API_UNAVAILABLE",
                                              "message"        : "Claude API call failed: connection timed out."
                                            }"""),
                            @ExampleObject(
                                    name = "CLAUDE_RESPONSE_INVALID — unparseable oracle response",
                                    value = """
                                            {
                                              "httpStatus"     : 502,
                                              "httpStatusText" : "Bad Gateway",
                                              "errorCode"      : "CLAUDE_RESPONSE_INVALID",
                                              "message"        : "Claude returned an unrecognised sentiment label: 'Neutral'. Expected POSITIVE or NEGATIVE."
                                            }""")
                    }))
    public ClassifyResponse submitReview(@Valid @RequestBody ReviewRequest request) {
        log.info("POST /api/reviews — classifying review of length: {}",
                request.getReviewText().length());
        return activeLearner.submitReview(request);
    }

    // -------------------------------------------------------------------------
    // GET /api/reviews
    // -------------------------------------------------------------------------

    /**
     * Returns all review samples stored in the system, labelled and
     * unlabelled alike.
     *
     * @return a list of all review samples; empty list if none exist
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List all review samples",
            description = "Returns every review sample in the system, " +
                    "regardless of labelling status."
    )
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully.")
    public List<ReviewSampleResponse> getAllReviews() {
        log.debug("GET /api/reviews — retrieving all samples.");
        return reviewSampleMapper.toResponseList(reviewSampleDao.findAll());
    }

    // -------------------------------------------------------------------------
    // GET /api/reviews/labelled
    // -------------------------------------------------------------------------

    /**
     * Returns all review samples that have been assigned a sentiment label,
     * regardless of label source.
     *
     * @return a list of labelled review samples; empty list if none exist
     */
    @GetMapping("/labelled")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List labelled review samples",
            description = "Returns only review samples that have been assigned a " +
                    "sentiment label — by seed data, manual input, or Claude oracle."
    )
    @ApiResponse(responseCode = "200", description = "Labelled reviews retrieved successfully.")
    public List<ReviewSampleResponse> getLabelledReviews() {
        log.debug("GET /api/reviews/labelled — retrieving labelled samples.");
        return reviewSampleMapper.toResponseList(reviewSampleDao.findAllLabelled());
    }

    // -------------------------------------------------------------------------
    // GET /api/reviews/unlabelled
    // -------------------------------------------------------------------------

    /**
     * Returns all review samples that have not yet been assigned a sentiment
     * label.
     *
     * @return a list of unlabelled review samples; empty list if none exist
     */
    @GetMapping("/unlabelled")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "List unlabelled review samples",
            description = "Returns only review samples that are still awaiting a label — " +
                    "either pending manual input or queued for Claude oracle labelling."
    )
    @ApiResponse(responseCode = "200", description = "Unlabelled reviews retrieved successfully.")
    public List<ReviewSampleResponse> getUnlabelledReviews() {
        log.debug("GET /api/reviews/unlabelled — retrieving unlabelled samples.");
        return reviewSampleMapper.toResponseList(reviewSampleDao.findAllUnlabelled());
    }

    // -------------------------------------------------------------------------
    // GET /api/reviews/{id}
    // -------------------------------------------------------------------------

    /**
     * Returns the review sample with the given identifier.
     *
     * @param id the database identifier of the review sample
     * @return the matching {@link ReviewSampleResponse}
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get a review sample by ID",
            description = "Returns the review sample with the given database identifier."
    )
    @ApiResponse(responseCode = "200", description = "Review sample found.")
    @ApiResponse(responseCode = "404",
            description = "Review sample not found.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(
                            name = "REVIEW_NOT_FOUND",
                            value = """
                                    {
                                      "httpStatus"     : 404,
                                      "httpStatusText" : "Not Found",
                                      "errorCode"      : "REVIEW_NOT_FOUND",
                                      "message"        : "Review sample with ID 42 was not found."
                                    }""")))
    public ReviewSampleResponse getReviewById(
            @Parameter(description = "The database identifier of the review sample.")
            @PathVariable Long id) {
        log.debug("GET /api/reviews/{} — retrieving sample.", id);
        return reviewSampleDao.findById(id)
                .map(reviewSampleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.REVIEW_NOT_FOUND,
                        "Review sample with ID " + id + " was not found."));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/reviews/{id}/label
    // -------------------------------------------------------------------------

    /**
     * Manually assigns a sentiment label to the review sample with the given
     * identifier.
     *
     * @param id      the database identifier of the review sample
     * @param request the label assignment body; must not be {@code null}
     * @return the updated {@link ReviewSampleResponse}
     */
    @PatchMapping(path = "/{id}/label", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Manually assign a sentiment label",
            description = "Assigns a POSITIVE or NEGATIVE label to the specified review sample. " +
                    "The label is stored with source MANUAL and overrides any existing label."
    )
    @ApiResponse(responseCode = "200", description = "Label applied successfully.")
    @ApiResponse(responseCode = "400",
            description = "Request validation failed.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(
                            name = "VALIDATION_ERROR — null label",
                            value = """
                                    {
                                      "httpStatus"     : 400,
                                      "httpStatusText" : "Bad Request",
                                      "errorCode"      : "VALIDATION_ERROR",
                                      "message"        : "Label must not be null. Accepted values: POSITIVE, NEGATIVE"
                                    }""")))
    @ApiResponse(responseCode = "404",
            description = "Review sample not found.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(
                            name = "REVIEW_NOT_FOUND",
                            value = """
                                    {
                                      "httpStatus"     : 404,
                                      "httpStatusText" : "Not Found",
                                      "errorCode"      : "REVIEW_NOT_FOUND",
                                      "message"        : "Review sample with ID 42 was not found."
                                    }""")))
    public ReviewSampleResponse labelReview(
            @Parameter(description = "The database identifier of the review sample.")
            @PathVariable Long id,
            @Valid @RequestBody LabelRequest request) {
        log.info("PATCH /api/reviews/{}/label — assigning label: {}", id, request.getLabel());
        return activeLearner.labelReview(id, request);
    }

    // -------------------------------------------------------------------------
    // POST /api/reviews/{id}/feedback
    // -------------------------------------------------------------------------

    /**
     * Submits a human correction to a model prediction for the review sample
     * with the given identifier.
     *
     * @param id      the database identifier of the misclassified review
     * @param request the correction details; must not be {@code null}
     * @return the updated {@link ReviewSampleResponse} with the corrected label
     */
    @PostMapping(path = "/{id}/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Submit a prediction correction",
            description = "Corrects a wrong model prediction. The corrected label is stored " +
                    "with source MANUAL and feeds into the next retraining cycle. " +
                    "An optional feedback note is logged for diagnostics."
    )
    @ApiResponse(responseCode = "200", description = "Correction applied successfully.")
    @ApiResponse(responseCode = "400",
            description = "Request validation failed.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "VALIDATION_ERROR — null corrected label",
                                    value = """
                                            {
                                              "httpStatus"     : 400,
                                              "httpStatusText" : "Bad Request",
                                              "errorCode"      : "VALIDATION_ERROR",
                                              "message"        : "Corrected label must not be null. Accepted values: POSITIVE, NEGATIVE"
                                            }"""),
                            @ExampleObject(
                                    name = "VALIDATION_ERROR — feedback note too long",
                                    value = """
                                            {
                                              "httpStatus"     : 400,
                                              "httpStatusText" : "Bad Request",
                                              "errorCode"      : "VALIDATION_ERROR",
                                              "message"        : "Feedback note must not exceed 500 characters"
                                            }""")
                    }))
    @ApiResponse(responseCode = "404",
            description = "Review sample not found.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(
                            name = "REVIEW_NOT_FOUND",
                            value = """
                                    {
                                      "httpStatus"     : 404,
                                      "httpStatusText" : "Not Found",
                                      "errorCode"      : "REVIEW_NOT_FOUND",
                                      "message"        : "Review sample with ID 42 was not found."
                                    }""")))
    public ReviewSampleResponse submitFeedback(
            @Parameter(description = "The database identifier of the misclassified review.")
            @PathVariable Long id,
            @Valid @RequestBody PredictionFeedbackRequest request) {
        log.info("POST /api/reviews/{}/feedback — corrected label: {}",
                id, request.getCorrectedLabel());
        return activeLearner.submitFeedback(id, request);
    }
}