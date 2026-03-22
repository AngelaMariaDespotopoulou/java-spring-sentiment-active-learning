# Sentiment Active Learning API — Data Models

This folder contains sample JSON files for every data model (schema) used by the **Sentiment Active Learning API**. Each file corresponds to exactly one schema visible in the Swagger UI and contains realistic sample data, field-level notes, and multiple named variants illustrating different business scenarios.

These files serve as a quick reference for developers integrating against the API without needing to open Swagger UI or run the application.

---

## 📁 Files in This Folder

### Request Models
Sent by the client **to** the API.

| File | Used by | Description |
|------|---------|-------------|
| `ReviewRequest.json` | `POST /api/reviews` | Submits a movie review for classification |
| `LabelRequest.json` | `PATCH /api/reviews/{id}/label` | Manually assigns a sentiment label |
| `PredictionFeedbackRequest.json` | `POST /api/reviews/{id}/feedback` | Corrects a wrong model prediction |
| `TrainingRequest.json` | `POST /api/model/train` | Triggers a manual training run |

### Response Models
Returned by the API **to** the client.

| File | Returned by | Description |
|------|-------------|-------------|
| `ReviewSampleResponse.json` | `GET /api/reviews`, `GET /api/reviews/{id}`, `GET /api/reviews/labelled`, `GET /api/reviews/unlabelled`, `PATCH /api/reviews/{id}/label`, `POST /api/reviews/{id}/feedback` | A review sample as stored in the database |
| `ClassifyResponse.json` | `POST /api/reviews` | Classification result including confidence score and uncertainty flag |
| `TrainingResponse.json` | `POST /api/model/train` | Training run result with corpus statistics and evaluation metrics |
| `ModelStatsResponse.json` | `GET /api/model/stats` | Live corpus counts combined with historical training metrics |
| `ActiveLearningStatusResponse.json` | `GET /api/model/active-learning-status` | Real-time active-learning cycle state and progress |
| `ApiErrorResponse.json` | All endpoints on error | Uniform error envelope used for every non-2xx response |

---

## 📖 How to Read the Sample Files

Each JSON file contains the following:

### `_description`
A plain-English explanation of what the model is, where it is used, and what purpose it serves.

### `_notes` / `_constraints`
Field-level annotations explaining validation rules, allowed values, nullability, and important behavioural notes. Particularly useful for fields that are nullable, have enums, or have non-obvious semantics.

### `_variant_*` blocks
Multiple named variants showing the same model in different business scenarios. For example, `ClassifyResponse.json` includes:
- `_variant_confident_positive` — model was sure, no oracle needed
- `_variant_confident_negative` — same, opposite label
- `_variant_uncertain_oracle_consulted` — model was unsure, Claude was asked

These variants are for **documentation purposes only** and are not valid request or response bodies themselves. The actual sample at the bottom of each file (without the `_` prefix) is the canonical example.

### Root-level fields (no `_` prefix)
The canonical sample — a single realistic, complete, valid JSON object representing the schema. This is the example you would use in integration tests or documentation.

> ℹ️ Fields prefixed with `_` are documentation metadata. They are not part of the actual API contract and would be ignored or rejected by the API if submitted. Remove them before using any request body in a real HTTP call.

---

## 📐 Schema Reference

### Request Schemas

#### `ReviewRequest`
```json
{
  "reviewText": "string (required, max 5000 characters)"
}
```
The only request model with a non-optional field. An empty string or a missing `reviewText` field produces a `VALIDATION_ERROR`.

---

#### `LabelRequest`
```json
{
  "label": "POSITIVE | NEGATIVE (required)"
}
```
Used to manually assign the highest-quality label to a review. A `MANUAL` label overrides any existing `SEED` or `CLAUDE` label on the same review.

---

#### `PredictionFeedbackRequest`
```json
{
  "correctedLabel": "POSITIVE | NEGATIVE (required)",
  "feedbackNote": "string (optional, max 500 characters)"
}
```
The `feedbackNote` is logged to the application log for diagnostic analysis but is not persisted to the database. It is the most important signal for identifying systematic model weaknesses.

---

#### `TrainingRequest`
```json
{
  "forceRetrain": false,
  "note": "string (optional)"
}
```
Both fields are optional. An entirely empty body (`{}`) or no body at all is also accepted — `forceRetrain` defaults to `false` and `note` defaults to `null`. Use `forceRetrain: true` with caution: training on fewer samples than the minimum threshold (`active-learning.min-training-samples`, default 20) may produce an unreliable model.

---

### Response Schemas

#### `ReviewSampleResponse`
```json
{
  "id": 42,
  "reviewText": "string",
  "label": "POSITIVE | NEGATIVE | null",
  "labelSource": "SEED | MANUAL | CLAUDE | null",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```
`label` and `labelSource` are both `null` for reviews submitted before the model was trained. `createdAt` is immutable — set on insert and never updated. `updatedAt` reflects the most recent change (e.g. a label being applied or corrected).

---

#### `ClassifyResponse`
```json
{
  "id": 42,
  "reviewText": "string",
  "label": "POSITIVE | NEGATIVE",
  "labelSource": "SEED | CLAUDE",
  "confidenceScore": 0.9412,
  "uncertain": false,
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```
Returned only by `POST /api/reviews`. Adds `confidenceScore` and `uncertain` to the base `ReviewSampleResponse`. When `uncertain` is `false`, `labelSource` is always `SEED` (the model was confident). When `uncertain` is `true`, `labelSource` is always `CLAUDE` (the oracle was consulted). The confidence threshold is configured via `active-learning.uncertainty-threshold` (default 0.65).

---

#### `TrainingResponse`
```json
{
  "trainedAt": "datetime",
  "samplesUsed": 165,
  "positiveCount": 89,
  "negativeCount": 76,
  "accuracyScore": 0.91,
  "precisionScore": 0.89,
  "recallScore": 0.93,
  "modelSavedToDisk": true,
  "modelStoragePath": "./model/sentiment-model.ser",
  "note": "string | null"
}
```
`samplesUsed` is the total across both training (80%) and hold-out (20%) sets. The evaluation metrics (`accuracyScore`, `precisionScore`, `recallScore`) are computed on the hold-out set only — data the model never saw during training. If `modelSavedToDisk` is `false`, the model is still live in memory for the current session but will not survive a restart.

---

#### `ModelStatsResponse`
```json
{
  "trained": true,
  "lastTrainedAt": "datetime | null",
  "samplesUsedInLastRun": 150,
  "totalLabelled": 165,
  "positiveCount": 89,
  "negativeCount": 76,
  "seedLabelledCount": 50,
  "manualLabelledCount": 30,
  "claudeLabelledCount": 85,
  "accuracyLastRun": 0.91,
  "retrainBatchSize": 10,
  "retrainBatchProgress": 7
}
```
Combines two data sources: live counts queried from the database at request time, and historical metrics from the most recent training run held in memory. `retrainBatchProgress` increments each time the Claude oracle is consulted and resets to zero after each automatic retraining run.

---

#### `ActiveLearningStatusResponse`
```json
{
  "cycleActive": true,
  "totalRetrainingRuns": 3,
  "lastRetrainedAt": "datetime | null",
  "claudeLabelledSinceLastRetrain": 7,
  "retrainBatchSize": 10,
  "progressPercent": 70,
  "totalUnlabelled": 4,
  "modelStoragePath": "./model/sentiment-model.ser",
  "modelFileExists": true
}
```
All counters (`totalRetrainingRuns`, `claudeLabelledSinceLastRetrain`) are in-memory and reset to zero on application restart. `modelFileExists` is checked against the filesystem at the time of the request — if the `.ser` file is missing, the current session works normally but the model will not survive a restart. `progressPercent` is computed as `claudeLabelledSinceLastRetrain / retrainBatchSize * 100`, capped at 100.

---

#### `ApiErrorResponse`
```json
{
  "httpStatus": 404,
  "httpStatusText": "Not Found",
  "errorCode": "REVIEW_NOT_FOUND",
  "message": "Review sample with ID 42 was not found."
}
```
Returned for **every** non-2xx response across all endpoints. When writing client code, branch on `errorCode` rather than `message` — the message string is human-readable and may change between versions. All ten error codes are documented with variants in `ApiErrorResponse.json`.

---

## ⚠️ Error Codes Quick Reference

| `errorCode` | HTTP | Trigger |
|-------------|------|---------|
| `VALIDATION_ERROR` | 400 | Field failed bean validation (blank, too long, null) |
| `INVALID_FIELD_VALUE` | 400 | Value is valid syntax but semantically wrong |
| `MALFORMED_REQUEST` | 400 | Request body is not valid JSON or Content-Type is missing |
| `REVIEW_NOT_FOUND` | 404 | No review exists for the given ID |
| `REVIEW_ALREADY_EXISTS` | 409 | Duplicate review text submitted |
| `MODEL_NOT_TRAINED` | 409 | Classification requested before model is trained |
| `INSUFFICIENT_TRAINING_DATA` | 409 | Training requested with fewer samples than minimum threshold |
| `CLAUDE_API_UNAVAILABLE` | 502 | Network error or timeout calling Anthropic Claude API |
| `CLAUDE_RESPONSE_INVALID` | 502 | Claude returned an unparseable response |
| `INTERNAL_ERROR` | 500 | Unexpected server-side error |

---

## 🔗 Additional Resources

- **Interactive API:** `http://localhost:8080/swagger-ui/index.html` (requires application running)
- **OpenAPI Specification:** see the `/swagger` folder for `api-docs.json` and the Swagger README
- **Postman Collection:** see the `/postman` folder to test all endpoints with pre-built requests
- **Author:** [Angela-Maria Despotopoulou](https://github.com/AngelaMariaDespotopoulou)
- **License:** [MIT](https://opensource.org/licenses/MIT)
