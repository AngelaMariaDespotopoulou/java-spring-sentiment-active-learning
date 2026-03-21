package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ModelNotTrainedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.data.text.TextFeatureExtractor;
import org.tribuo.data.text.impl.BasicPipeline;
import org.tribuo.data.text.impl.TextFeatureExtractorImpl;
import org.tribuo.util.tokens.universal.UniversalTokenizer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Production implementation of {@link Classifier} backed by a Tribuo
 * Multinomial Naive Bayes model.
 *
 * <p>The trained {@link Model} is held in an {@link AtomicReference} to
 * guarantee thread-safe reads and writes. Multiple concurrent HTTP requests
 * may call {@link #classify(String)} simultaneously — the atomic reference
 * ensures they always see a consistent model state without locking the entire
 * service. A model swap via {@link #updateModel(Model)} is also atomic —
 * in-flight requests using the old model complete normally while new requests
 * immediately use the replacement.
 *
 * <h2>Feature extraction</h2>
 * <p>Raw review text is converted into a numerical feature vector using
 * Tribuo's {@link TextFeatureExtractorImpl} backed by a {@link BasicPipeline}
 * with a {@link UniversalTokenizer}. This produces unigram bag-of-words
 * features. The same extractor configuration is used during both training and
 * prediction, ensuring that the feature space is consistent — a critical
 * requirement for correct predictions.
 *
 * <h2>Uncertainty detection</h2>
 * <p>After each prediction, the confidence score (the probability Tribuo
 * assigned to the winning label) is compared against the configured
 * uncertainty threshold (see {@link ActiveLearningProps}, property
 * {@code active-learning.uncertainty-threshold}). If the score falls
 * below the threshold, the result is flagged as uncertain and the caller
 * — {@code ActiveLearningService} — is responsible for consulting the Claude
 * AI oracle.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassifierService implements Classifier {

    /**
     * Active-learning configuration properties.
     * Used to read the uncertainty threshold at classification time.
     */
    private final ActiveLearningProps activeLearningProps;

    /**
     * Thread-safe reference to the currently trained Tribuo model.
     *
     * <p>Initialised to {@code null} — meaning untrained. Updated atomically
     * by {@link #updateModel(Model)} after each successful training run.
     * Read atomically by {@link #classify(String)} on every request.
     */
    private final AtomicReference<Model<Label>> modelRef = new AtomicReference<>(null);

    /**
     * Tribuo text feature extractor used to convert raw review text into a
     * numerical feature vector.
     *
     * <p>Uses a {@link UniversalTokenizer} fed through a {@link BasicPipeline}
     * that builds unigram bag-of-words features. Must be the same extractor
     * configuration used during training to ensure the feature space is
     * consistent between training and prediction.
     */
    private final TextFeatureExtractor<Label> featureExtractor = buildFeatureExtractor();

    // -------------------------------------------------------------------------
    // Classifier interface implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: extracts bag-of-words features from the review
     * text using {@link #featureExtractor}, feeds the resulting
     * {@link Example} to the Tribuo model, reads the top predicted
     * {@link Label} and its probability, then compares the probability against
     * the configured uncertainty threshold.
     */
    @Override
    public Classifier.ClassificationResult classify(String reviewText) {
        Model<Label> model = modelRef.get();

        if (model == null) {
            throw new ModelNotTrainedException(
                    "The model has not been trained yet. " +
                            "Please submit labelled samples and trigger a training run first.");
        }

        Example<Label> example = featureExtractor.extract(
                new Label("UNKNOWN"), reviewText);

        Prediction<Label> prediction = model.predict(example);

        String predictedLabelStr = prediction.getOutput().getLabel();
        double confidenceScore   = prediction.getOutput().getScore();
        boolean uncertain        = confidenceScore < activeLearningProps.getUncertaintyThreshold();

        SentimentLabel label = SentimentLabel.valueOf(predictedLabelStr);

        log.debug("Classified review — label: {}, confidence: {:.4f}, uncertain: {}",
                label, confidenceScore, uncertain);

        return new Classifier.ClassificationResult(label, confidenceScore, uncertain);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrained() {
        return modelRef.get() != null;
    }

    /** {@inheritDoc} */
    @Override
    public void updateModel(Model<Label> newModel) {
        Model<Label> previous = modelRef.getAndSet(newModel);
        if (previous == null) {
            log.info("Model initialised for the first time.");
        } else {
            log.info("Model hot-swapped — previous model replaced with newly trained model.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Model<Label> getCurrentModel() {
        return modelRef.get();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the {@link TextFeatureExtractor} used to convert raw
     * review text into a Tribuo {@link Example} feature vector.
     *
     * <p>Uses a {@link UniversalTokenizer} (a robust tokeniser handling
     * punctuation, whitespace and CJK characters) fed through a
     * {@link BasicPipeline} that produces unigram bag-of-words features.
     * This is the simplest feature representation compatible with Multinomial
     * Naive Bayes and is well suited to short movie review texts.
     *
     * <p>This method is called once at bean construction time. The same
     * extractor instance is reused for every classification request, which is
     * safe because {@link TextFeatureExtractor} implementations in Tribuo are
     * stateless after construction.
     *
     * @return a configured {@link TextFeatureExtractorImpl} for bag-of-words
     *         feature extraction
     */
    private TextFeatureExtractor<Label> buildFeatureExtractor() {
        return new TextFeatureExtractorImpl<>(
                new BasicPipeline(new UniversalTokenizer(), 1));
    }
}