package io.github.amdespotopoulou.sentimentactivelearning.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed configuration properties for the active-learning pipeline.
 *
 * <p>This class binds all {@code active-learning.*} keys defined in
 * {@code application.properties} into a single validated bean. Any misconfiguration
 * — a threshold outside the 0.0–1.0 range, a batch size of zero, etc. — causes
 * the application to fail fast at startup with a clear error message rather than
 * producing silent incorrect behaviour at runtime.
 *
 * <h2>Available properties</h2>
 * <pre>
 * active-learning.uncertainty-threshold      (double, 0.0-1.0, default 0.65)
 * active-learning.retrain-batch-size         (int,    min 1,   default 10)
 * active-learning.min-training-samples       (int,    min 2,   default 20)
 * active-learning.model-storage-path         (String, required, from env var)
 * active-learning.retrain-on-startup         (boolean,          default true)
 * active-learning.staleness-threshold-samples(int,    min 1,   default 5)
 * </pre>
 *
 * <h2>Tuning guidance</h2>
 * <ul>
 *   <li><b>uncertainty-threshold</b> — raise to make the model ask Claude more
 *       often (more Claude calls, higher quality labels); lower to make the model
 *       more self-reliant (fewer Claude calls, model relies more on its own
 *       confidence). A value of {@code 0.65} means the model defers to Claude
 *       whenever it is less than 65% confident in its prediction.</li>
 *   <li><b>retrain-batch-size</b> — raise to retrain less frequently (cheaper,
 *       but the model stays stale longer); lower to retrain more aggressively
 *       (fresher model, but higher CPU cost per request batch).</li>
 *   <li><b>min-training-samples</b> — the absolute minimum number of labelled
 *       samples required before the first training run. Below this count the
 *       model refuses to train and returns a
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode#INSUFFICIENT_TRAINING_DATA}
 *       error. Must be at least 2 (one per class) for Naive Bayes to function.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "active-learning")
public class ActiveLearningProps {

    /**
     * Confidence threshold below which a prediction is considered uncertain
     * and forwarded to the Claude AI oracle for labelling.
     *
     * <p>Range: {@code 0.0} (never defer to Claude) to {@code 1.0} (always defer
     * to Claude). A value of {@code 0.65} means: if the model is less than 65%
     * confident, ask Claude. Defaults to {@code 0.65}.
     */
    @DecimalMin(value = "0.0", message = "active-learning.uncertainty-threshold must be at least 0.0")
    @DecimalMax(value = "1.0", message = "active-learning.uncertainty-threshold must be at most 1.0")
    private double uncertaintyThreshold = 0.65;

    /**
     * Number of new Claude-labelled samples that must accumulate before the
     * model is retrained.
     *
     * <p>Retraining on every single new label would be computationally wasteful.
     * Batching amortises the cost across multiple new samples. A value of
     * {@code 10} means the model retrains after every tenth Claude-labelled
     * sample. Defaults to {@code 10}.
     */
    @Min(value = 1, message = "active-learning.retrain-batch-size must be at least 1")
    private int retrainBatchSize = 10;

    /**
     * Minimum number of labelled samples required before the first training run
     * can be executed.
     *
     * <p>Naive Bayes requires at least one example per class to compute meaningful
     * probabilities. In practice a much larger corpus produces a more reliable
     * model. A value of {@code 20} ensures at least a modest initial training set.
     * Defaults to {@code 20}.
     */
    @Min(value = 2, message = "active-learning.min-training-samples must be at least 2 (one per sentiment class)")
    private int minTrainingSamples = 20;

    /**
     * Full path to the file where the trained Tribuo model is serialised after
     * each training run.
     *
     * <p>Must point to a location on a volume that survives container restarts
     * (a bind-mounted host directory in Docker). Resolved from the
     * {@code MODEL_STORAGE_PATH} environment variable; never hardcoded in any
     * committed file.
     *
     * <p>Example: {@code /var/data/sentiment/sentiment-model.ser}
     */
    @NotBlank(message = "active-learning.model-storage-path must not be blank")
    private String modelStoragePath;

    /**
     * Whether to attempt model loading or retraining automatically when the
     * application starts.
     *
     * <p>When {@code true}, the startup sequence follows the Option 3 strategy:
     * load the serialised model from disk if it exists, retrain if it is stale
     * or missing, or wait silently if there is insufficient labelled data.
     * When {@code false}, the application starts in an untrained state
     * regardless of what is on disk or in the database. Defaults to {@code true}.
     */
    private boolean retrainOnStartup = true;

    /**
     * Number of new labelled samples added to the database since the model was
     * last serialised to disk that cause the loaded model to be considered stale.
     *
     * <p>On startup, if a serialised model file exists but this many or more
     * new labels have been added since it was saved, the model is retrained
     * from scratch rather than loaded from disk. A lower value keeps the model
     * fresher at the cost of more frequent startup retraining. Resolved from
     * the {@code MODEL_STALENESS_THRESHOLD} environment variable in production.
     * Defaults to {@code 5}.
     */
    @Min(value = 1, message = "active-learning.staleness-threshold-samples must be at least 1")
    private int stalenessThresholdSamples = 5;
}