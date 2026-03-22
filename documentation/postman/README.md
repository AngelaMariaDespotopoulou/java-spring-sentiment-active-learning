# Sentiment Active Learning API — Postman Collection

This folder contains a complete [Postman](https://www.postman.com/) collection for testing the **Sentiment Active Learning API**. It covers every endpoint, includes success and error response examples, runs automated tests on every request, and provides a seed data workflow to get the application into a testable state within minutes.

---

## 📁 Files in This Folder

| File | Description |
|------|-------------|
| `Sentiment-Active-Learning-API.postman_collection.json` | The full API collection — all endpoints, examples, and test scripts |
| `SAL-Local-Dev.postman_environment.json` | Environment for local development (`localhost:8080`, default credentials) |
| `SAL-Production.postman_environment.json` | Environment template for production deployments (fill in your values before use) |
| `README.md` | This file |

---

## 🧰 Prerequisites

### 1. Install Postman
Download and install [Postman Desktop](https://www.postman.com/downloads/) (free). The collection uses Postman Collection v2.1 format which is supported by all recent versions.

> ℹ️ You can also use the [Postman Web interface](https://web.postman.co/) if you prefer not to install the desktop app, but the desktop version is recommended for local API testing.

### 2. Have the Application Running
The application must be running locally before you import and use this collection.

Start it from the project root:
```bash
mvn spring-boot:run
```

Or from IntelliJ IDEA: right-click `SentimentActiveLearningApplication.java` → **Run**.

Verify it is up by visiting:
```
http://localhost:8080/actuator/health
```
You should see `{"status":"UP"}`.

---

## 📥 Importing the Collection and Environments

### Step 1 — Open Postman
Launch the Postman desktop application and sign in (or continue without signing in).

### Step 2 — Import the Collection
1. Click the **Import** button in the top-left area of the Postman window.
2. In the dialog that opens, click **Upload Files**.
3. Navigate to this `postman/` folder and select:
   ```
   Sentiment-Active-Learning-API.postman_collection.json
   ```
4. Click **Import**.

You will see a new collection called **Sentiment Active Learning API** appear in your left sidebar.

### Step 3 — Import the Environments
Repeat the import process for the environment files:

1. Click **Import** again.
2. Select `SAL-Local-Dev.postman_environment.json`.
3. Click **Import**.
4. Repeat for `SAL-Production.postman_environment.json` if you need it.

### Step 4 — Select the Active Environment
1. In the top-right corner of Postman, click the environment dropdown (it may say **No Environment**).
2. Select **SAL — Local Dev**.

You should now see the environment variables `baseUrl`, `username`, `password`, and `lastReviewId` populate automatically.

---

## 🔐 Authentication

All API endpoints are protected with **HTTP Basic Authentication**. The collection is pre-configured to send credentials automatically on every request using collection-level auth — you do not need to set credentials per-request.

Credentials are stored as environment variables:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `{{username}}` | `admin` | Swagger UI username |
| `{{password}}` | `admin` | Swagger UI password |

To change the credentials:
1. Click the **Environments** icon (eye symbol) in the left sidebar.
2. Click **SAL — Local Dev**.
3. Edit the `username` and `password` values to match your `application-dev.properties`.

> ⚠️ **Never commit real production credentials** to source control. The `SAL-Production.postman_environment.json` file intentionally has empty credential fields — fill them in locally and do not push the filled version.

---

## 📋 Collection Structure

The collection is organised into four folders:

```
Sentiment Active Learning API
├── 🏥 Health
│   └── Health Check
├── 🌱 Seed Data            ← run this first to populate the database
│   ├── Seed — Positive 01 through 10
│   ├── Seed — Negative 01 through 10
│   └── Label All Seed Reviews — Positive Batch (reminder request)
├── 📝 Reviews
│   ├── Submit Review for Classification     POST /api/reviews
│   ├── List All Reviews                     GET  /api/reviews
│   ├── List Labelled Reviews                GET  /api/reviews/labelled
│   ├── List Unlabelled Reviews              GET  /api/reviews/unlabelled
│   ├── Get Review by ID                     GET  /api/reviews/{{lastReviewId}}
│   ├── Manually Assign Label                PATCH /api/reviews/{{lastReviewId}}/label
│   └── Submit Prediction Feedback           POST /api/reviews/{{lastReviewId}}/feedback
└── 🤖 Model
    ├── Trigger Manual Training Run          POST /api/model/train
    ├── Trigger Training — Force Retrain     POST /api/model/train (forceRetrain: true)
    ├── Get Model Statistics                 GET  /api/model/stats
    └── Get Active Learning Status           GET  /api/model/active-learning-status
```

---

## 🚀 Recommended First-Run Workflow

Follow these steps in order for the best experience on a fresh application start.

### Step 1 — Health Check
Open **🏥 Health → Health Check** and click **Send**.

You should receive:
```json
{ "status": "UP" }
```

If you see a connection error, the application is not running. Start it first.

---

### Step 2 — Seed the Database
The application starts with an empty database and needs at least **20 labelled samples** to train the model (this is the default minimum — configurable in `application-dev.properties`).

The **🌱 Seed Data** folder contains 20 pre-written movie reviews (10 positive, 10 negative). The easiest way to run them all at once is with the **Collection Runner**:

1. Click the **🌱 Seed Data** folder name in the sidebar.
2. Click the **Run** button (▷) that appears.
3. In the Runner panel that opens:
   - Make sure **SAL — Local Dev** is selected as the environment.
   - Leave iteration count at `1`.
   - Click **Run Sentiment Active Learning API**.
4. All 20 requests will execute in sequence. Each should show a green `201 Created` status.

> ℹ️ **Manually labelling the seed reviews:** After seeding, the reviews are stored in the database but are not yet labelled (the model is not trained yet so classification is not possible). Use **PATCH /api/reviews/:id/label** to assign `POSITIVE` or `NEGATIVE` labels to each review. First call **GET /api/reviews** to find the assigned IDs, then label each one.

---

### Step 3 — Train the Model
Once at least 20 reviews are labelled, open **🤖 Model → Trigger Manual Training Run** and click **Send**.

You should receive a response like:
```json
{
  "trainedAt": "2026-03-22T16:00:00",
  "samplesUsed": 20,
  "accuracyScore": 0.85,
  "modelSavedToDisk": true
}
```

The model is now live. The Naive Bayes classifier will immediately begin classifying submitted reviews.

> 💡 If you have fewer than 20 labelled samples and want to test anyway, use **Trigger Training — Force Retrain** which bypasses the minimum threshold.

---

### Step 4 — Submit a Review for Classification
Open **📝 Reviews → Submit Review for Classification** and click **Send**.

The request body contains a sample review. The model will classify it and return:
```json
{
  "id": 42,
  "label": "POSITIVE",
  "labelSource": "SEED",
  "confidenceScore": 0.9412,
  "uncertain": false
}
```

- If `uncertain: false` — the model was confident. The label was assigned automatically.
- If `uncertain: true` — the model's confidence was below the threshold. Claude AI was consulted and its label was used instead (`labelSource: "CLAUDE"`).

The `id` returned here is **automatically saved** as `{{lastReviewId}}` by a post-response script. You do not need to copy it manually — all subsequent requests that use an ID will pick it up automatically.

---

### Step 5 — Monitor the Active Learning Cycle
Open **🤖 Model → Get Active Learning Status** and click **Send**.

This shows you:
- Whether the cycle is active
- How many Claude-labelled samples have accumulated since the last retrain
- Progress towards the next automatic retraining run (as a percentage)
- Whether the model file exists on disk

---

## 🧪 Automated Tests

Every request in this collection has automated test scripts that run after each response. You can see the results in the **Test Results** tab below the response body.

### Collection-level tests (run on every request)
- Response time is under 5000ms
- Content-Type is `application/json`

### Request-level tests (specific to each endpoint)
- Correct HTTP status code
- Required fields present in response body
- Field types and value ranges (e.g. `confidenceScore` is between 0 and 1)
- Business rules (e.g. labelled reviews all have a non-null `label`)
- Dynamic variable capture (e.g. `lastReviewId` saved after POST)

### Running all tests at once
Use the **Collection Runner** to run the entire collection and see a test report:
1. Click the collection name **Sentiment Active Learning API**.
2. Click **Run**.
3. Select the environment and click **Run Sentiment Active Learning API**.
4. All tests will run and a pass/fail report will be displayed.

---

## 📖 Understanding the Response Examples

Every request includes saved **response examples** — realistic sample responses for both success and error cases. To view them:

1. Click any request to open it.
2. Click the **Examples** dropdown (top-right of the request panel, next to **Send**).
3. Select any example to see the expected response body and status code.

Error responses always follow the **uniform error envelope**:
```json
{
  "httpStatus": 409,
  "httpStatusText": "Conflict",
  "errorCode": "MODEL_NOT_TRAINED",
  "message": "The model has not been trained yet. Please submit labelled samples and trigger a training run first."
}
```

The `errorCode` field is the machine-readable identifier used by client code. The `message` field is human-readable and safe to display to end users.

---

## 🔄 Dynamic Variables

The collection uses variables that are automatically updated during your session:

| Variable | Set by | Used by |
|----------|--------|---------|
| `{{baseUrl}}` | Environment file | Every request |
| `{{username}}` | Environment file | Collection-level auth |
| `{{password}}` | Environment file | Collection-level auth |
| `{{lastReviewId}}` | POST /api/reviews (post-response script) | GET /:id, PATCH /:id/label, POST /:id/feedback |

To inspect or override any variable at any time:
1. Click the **eye icon** (🔍) in the top-right corner of Postman.
2. The current values of all environment and collection variables are displayed.
3. Click **Edit** to change any value manually.

---

## 🌍 Switching to Production

To test against a production deployment:

1. Select **SAL — Production** from the environment dropdown.
2. Click the environment name to edit it.
3. Fill in:
   - `baseUrl` — your production host URL (e.g. `https://api.yourhost.com`)
   - `username` — your production Swagger admin username
   - `password` — your production Swagger admin password
4. Click **Save**.

All requests will now target the production environment.

> ⚠️ Exercise caution when running the **🌱 Seed Data** folder or **POST /api/model/train** against production — these modify the database and the live model.

---

## 🐛 Troubleshooting

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| `Could not send request` | Application not running | Start the application: `mvn spring-boot:run` |
| `401 Unauthorized` | Wrong credentials | Check `username`/`password` in your environment match `application-dev.properties` |
| `409 MODEL_NOT_TRAINED` | No trained model | Run the Seed Data folder, label reviews, then POST /api/model/train |
| `409 INSUFFICIENT_TRAINING_DATA` | Too few labelled samples | Seed and label more reviews, or use **Force Retrain** |
| `409 REVIEW_ALREADY_EXISTS` | Duplicate review text | Change the review text in the request body |
| `502 CLAUDE_API_UNAVAILABLE` | Missing or invalid Claude API key | Check `claude.api.key` in `application-dev.properties` |
| `{{lastReviewId}}` shows as literal text | No review submitted yet | Run **Submit Review** first to populate the variable |
| Tests failing after restart | H2 is in-memory, data is gone | Restart clears the database — re-run the seed workflow |

---

## 📬 Contact

**Angela-Maria Despotopoulou**
GitHub: [https://github.com/AngelaMariaDespotopoulou](https://github.com/AngelaMariaDespotopoulou)

---

*Collection version: 1.0.0 — compatible with Postman 10.x and above.*
