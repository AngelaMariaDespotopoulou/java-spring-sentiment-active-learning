# Sentiment Active Learning — Architecture Diagrams

This folder contains PlantUML source files and rendered PNG images documenting the architecture, data model, and runtime behaviour of the **Sentiment Active Learning** application.

All diagrams are generated from the `.puml` source files using [PlantUML](https://plantuml.com/). The PNG exports are provided as a convenience — the `.puml` files are the authoritative source and should be updated whenever the codebase changes.

---

## 📁 Files in This Folder

| Source (`.puml`) | Rendered (`.png`) | Description |
|------------------|-------------------|-------------|
| `active-learning-cycle.puml` | `active_learning_cycle-Active_Learning_Cycle__Review_Submission_Flow.png` | Sequence diagram of the full active-learning cycle from review submission to automatic retraining |
| `architecture.puml` | `architecture-Sentiment_Active_Learning__Component_Architecture.png` | Component diagram showing all layers, beans, and their runtime dependencies |
| `entity-model.puml` | `entity_model-Entity_Model___Persistence_Layer.png` | Class diagram of the persistence layer — entity, enums, DAO interface, implementation, and repository |
| `startup-sequence.puml` | `startup_sequence-Startup_Model_Restoration_Sequence.png` | Activity diagram of the three-branch startup model restoration sequence |

---

## 🖼️ Diagram Descriptions

### 1. Active Learning Cycle — Review Submission Flow

**File:** `active-learning-cycle.puml`

This is the most important diagram in the project. It traces the complete lifecycle of a single review submission through every component in the system, from the initial HTTP request to the final response — including the automatic retraining trigger.

**What it shows:**

The diagram is divided into four labelled sections:

- **Review Submission** — the review text is received by `ReviewController`, passed to `ActiveLearningService`, and immediately persisted to the H2 database via `ReviewSampleDao` before any classification is attempted. This guarantees the review is never lost even if classification fails.

- **Classification** — `ClassifierService` extracts bag-of-words features using `UniversalTokenizer` and `BasicPipeline`, then runs `model.predict()`. The result is a label and a confidence score.

- **Claude Oracle Consultation** — shown only when the classifier's confidence falls below the configured `uncertaintyThreshold` (default 0.65). `ClaudeOracleService` sends the review text to the Anthropic Claude API and parses the single-word response (`POSITIVE` or `NEGATIVE`). The `claudeLabelledSinceLastRetrain` counter is incremented.

- **Automatic Retraining** — triggered when `claudeLabelledSinceLastRetrain` reaches the configured `retrainBatchSize`. `TrainingService` fetches all labelled samples, shuffles and splits them 80/20, builds a Tribuo `MutableDataset`, trains `MultinomialNaiveBayesTrainer`, evaluates on the hold-out set, hot-swaps the live model in `ClassifierService`, and saves the result to disk.

**Branching paths shown:**
- Confident prediction → label saved as `SEED`, response returned immediately
- Uncertain prediction, batch threshold not yet reached → Claude labels, counter incremented, response returned
- Uncertain prediction, batch threshold reached → Claude labels, automatic retrain triggered, updated model goes live, response returned

---

### 2. Component Architecture

**File:** `architecture.puml`

A static component diagram showing every class in the application, which layer it belongs to, and how the layers interact at runtime. Solid arrows represent direct method calls; dashed arrows represent Spring dependency injection (configuration feeding into components).

**Layers shown:**

| Layer | Components |
|-------|------------|
| **Configuration** | `ClaudeClientConfig`, `OpenApiConfig`, `SecurityConfig`, `ActiveLearningProps` |
| **API Layer** | `ReviewController`, `ModelController` |
| **Service Layer — Core** | `ActiveLearningService`, `ClassifierService`, `TrainingService` |
| **Service Layer — Oracle** | `ClaudeOracleService`, `ClaudeRequestMapper` |
| **Persistence Layer** | `ReviewSampleDaoImpl`, `ReviewSampleRepository` |
| **External** | H2 Database, Anthropic Claude API, Model File (`.ser`) |

**Key design decisions visible in the diagram:**

- Controllers never call the DAO directly — all persistence goes through the service layer
- `TrainingService` both serialises the model to disk (solid arrow) and deserialises it at startup (dashed arrow)
- `ClaudeOracleService` is only ever called by `ActiveLearningService` — never by controllers
- Configuration beans feed into components via dashed arrows, keeping runtime calls clean

---

### 3. Entity Model & Persistence Layer

**File:** `entity-model.puml`

A class diagram covering the full persistence stack — from the JPA entity and its associated enumerations, through the technology-agnostic DAO interface, to the Spring Data JPA repository.

**What it shows:**

- **`ReviewSample`** (entity) — the sole JPA entity in the application. Stored in the `review_sample` table with a unique constraint on `reviewText`. Audit timestamps (`createdAt`, `updatedAt`) are managed automatically by `AuditingEntityListener`. Entity lifecycle events (insert, update) are logged by `ReviewSampleListener`.

- **`SentimentLabel`** (enum) — `POSITIVE` or `NEGATIVE`. Stored as an SQL enum column. Nullable — a review exists in the database before it is classified.

- **`LabelSource`** (enum) — `SEED`, `MANUAL`, or `CLAUDE`. Records the origin of every label for quality tracking and corpus analysis.

- **`ErrorCode`** (enum) — all ten application error codes with their associated HTTP status. Included here because it is tightly coupled to the API contract and useful to see alongside the domain model.

- **`ReviewSampleDao`** (interface) — the technology-agnostic DAO contract. The service layer depends exclusively on this interface — it never imports the repository directly. This is the DAO pattern: if the persistence technology ever changes, only `ReviewSampleDaoImpl` needs to change.

- **`ReviewSampleDaoImpl`** (class) — the JPA-backed implementation. Adds duplicate-checking logic before `save()` and existence-checking logic before `update()`, then delegates all queries to `ReviewSampleRepository`.

- **`ReviewSampleRepository`** (interface) — extends Spring Data's `JpaRepository`. Never called directly by the service layer. Provides derived queries for labelled/unlabelled filtering, label source counts, and existence checks.

---

### 4. Startup Model Restoration Sequence

**File:** `startup-sequence.puml`

An activity diagram showing the three-branch decision tree that `TrainingService` executes when the application starts, triggered by Spring's `ApplicationReadyEvent`.

**Why `ApplicationReadyEvent` and not `@PostConstruct`?**

`@PostConstruct` fires during bean initialisation, before the JPA layer is ready. `ApplicationReadyEvent` fires after the full application context — including the datasource and all repositories — is available. Using the wrong hook would cause the startup query to fail before Hibernate has initialised.

**The three startup outcomes:**

| Condition | Outcome |
|-----------|---------|
| `retrainOnStartup = false` | Startup restoration disabled. Application starts untrained. |
| Model file exists AND `newLabelsSinceSave < stalenessThreshold` | Model deserialised from disk. Fast startup — no training run needed. |
| Model file exists AND `newLabelsSinceSave ≥ stalenessThreshold` | Saved model is stale. Retrain from database, save result. |
| No model file AND `labelledCount ≥ minTrainingSamples` | No saved model but data available. Train from scratch, save result. |
| No model file AND `labelledCount < minTrainingSamples` | Insufficient data. Start in untrained state and wait. This is normal for a fresh deployment. |

**Staleness detection mechanism:**

When `TrainingService` saves a model to disk, it writes two objects in sequence: the serialised `Model<Label>` object followed by a `long` representing the current labelled sample count. At startup, it reads both back, computes the difference between the current count and the saved count, and uses that difference to decide whether the model is still representative of the data.

---

## 🔧 Regenerating the PNG Exports

The PNG files were generated from the `.puml` source files using PlantUML. To regenerate them after making changes:

### Option A — IntelliJ IDEA (recommended)
1. Install the [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) plugin.
2. Open any `.puml` file — a live preview appears automatically.
3. Right-click the preview → **Export Diagram** → choose PNG.

### Option B — VS Code
1. Install the [PlantUML extension](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml) by jebbs.
2. Open a `.puml` file and press `Alt+D` to preview.
3. Press `Ctrl+Shift+P` → **PlantUML: Export Current Diagram**.

### Option C — Command Line
Requires Java and the PlantUML JAR:
```bash
# Download the JAR once
curl -L -o plantuml.jar https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar

# Regenerate all diagrams in this folder
java -jar plantuml.jar -tpng docs/diagrams/*.puml
```

### Option D — Online
Paste the contents of any `.puml` file into [plantuml.com/server](https://www.plantuml.com/plantuml/uml/) and download the PNG.

---

## 📌 Keeping Diagrams Up To Date

Diagrams should be updated whenever:

- A new service, controller, or configuration class is added
- The active-learning cycle logic changes (new branching paths, new thresholds)
- The entity model changes (new fields, new enumerations)
- The startup sequence changes (new conditions, new fallback behaviour)

A diagram that is out of sync with the code is worse than no diagram — it actively misleads readers. Treat the `.puml` files as living documentation and commit them alongside the code changes they reflect.

---

## 🔗 Additional Resources

- **Postman Collection:** see the `/postman` folder for API test requests
- **Swagger Documentation:** see the `/swagger` folder for the OpenAPI spec and Swagger UI screenshot
- **Author:** [Angela-Maria Despotopoulou](https://github.com/AngelaMariaDespotopoulou)
- **License:** [MIT](https://opensource.org/licenses/MIT)
