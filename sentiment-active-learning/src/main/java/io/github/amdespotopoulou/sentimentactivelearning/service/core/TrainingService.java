package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.TrainingRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.TrainingResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.tribuo.Dataset;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.mnb.MultinomialNaiveBayesTrainer;
import org.tribuo.data.text.TextFeatureExtractor;
import org.tribuo.data.text.impl.BasicPipeline;
import org.tribuo.data.text.impl.TextFeatureExtractorImpl;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.util.tokens.universal.UniversalTokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.List;

/**
 * Production implementation of {@link ModelTrainer} backed by Tribuo's
 * Multinomial Naive Bayes algorithm.
 *
 * <h2>Training pipeline</h2>
 * <p>Each training run follows these steps:
 * <ol>
 *   <li>Guard: verify the labelled sample count meets the configured minimum
 *       (unless {@code forceRetrain} is requested).</li>
 *   <li>Fetch: retrieve all labelled {@link ReviewSample} records from the
 *       DAO.</li>
 *   <li>Split: shuffle and divide into an 80% training set and a 20%
 *       hold-out evaluation set.</li>
 *   <li>Extract: convert each review text into a bag-of-words
 *       {@link Example} using {@link TextFeatureExtractorImpl} backed by
 *       {@link BasicPipeline} and {@link UniversalTokenizer}.</li>
 *   <li>Train: feed the training dataset to
 *       {@link MultinomialNaiveBayesTrainer}.</li>
 *   <li>Evaluate: compute accuracy, precision and recall on the hold-out
 *       set using Tribuo's {@link LabelEvaluator}.</li>
 *   <li>Swap: atomically replace the live model in {@link Classifier} via
 *       {@link Classifier#updateModel(Model)}.</li>
 *   <li>Persist: serialise the trained model to the configured storage
 *       path using Java object serialisation.</li>
 * </ol>
 *
 * <h2>Startup sequence</h2>
 * <p>On {@link ApplicationReadyEvent}, {@link #onStartup()} attempts to
 * restore the model without requiring a fresh training run:
 * <ol>
 *   <li>If a model file exists and is not stale, deserialise and load it.</li>
 *   <li>If the file is stale or missing but data is available, retrain.</li>
 *   <li>Otherwise start untrained and wait for labelled data.</li>
 * </ol>
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService implements ModelTrainer {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Fraction of labelled samples reserved for the hold-out evaluation set.
     * The remaining fraction is used for training.
     */
    private static final double EVAL_SPLIT_RATIO = 0.20;

    /**
     * Minimum number of examples required in the hold-out evaluation set.
     * If the dataset is too small to produce this many evaluation examples,
     * evaluation is skipped and metrics are reported as zero.
     */
    private static final int MIN_EVAL_EXAMPLES = 2;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /**
     * Active-learning configuration properties: thresholds, storage path,
     * startup behaviour.
     */
    private final ActiveLearningProps activeLearningProps;

    /**
     * Data access object for reading labelled review samples.
     */
    private final ReviewSampleDao reviewSampleDao;

    /**
     * The live classifier bean. Receives the newly trained model via
     * {@link Classifier#updateModel(Model)} after each successful run.
     */
    private final Classifier classifier;

    // -------------------------------------------------------------------------
    // Historical state (thread-safe)
    // -------------------------------------------------------------------------

    /**
     * Timestamp of the most recent completed training run.
     * {@code null} until the first training run completes successfully.
     */
    private final AtomicReference<LocalDateTime> lastTrainedAt =
            new AtomicReference<>(null);

    /**
     * Number of labelled samples used in the most recent training run.
     * Zero until the first training run completes.
     */
    private final AtomicReference<Long> samplesUsedInLastRun =
            new AtomicReference<>(0L);

    /**
     * Accuracy score achieved on the hold-out evaluation set during the most
     * recent training run. Zero until the first training run completes.
     */
    private final AtomicReference<Double> accuracyLastRun =
            new AtomicReference<>(0.0);

    /**
     * Precision score achieved on the hold-out evaluation set during the most
     * recent training run. Zero until the first training run completes.
     */
    private final AtomicReference<Double> precisionLastRun =
            new AtomicReference<>(0.0);

    /**
     * Recall score achieved on the hold-out evaluation set during the most
     * recent training run. Zero until the first training run completes.
     */
    private final AtomicReference<Double> recallLastRun =
            new AtomicReference<>(0.0);

    // -------------------------------------------------------------------------
    // ModelTrainer interface implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: shuffles all labelled samples, splits 80/20
     * into training and evaluation sets, extracts bag-of-words features,
     * trains a {@link MultinomialNaiveBayesTrainer}, evaluates on the
     * hold-out set, hot-swaps the live model, and serialises to disk.
     */
    @Override
    public TrainingResponse train(TrainingRequest request) {
        log.info("Training run requested. forceRetrain={}, note='{}'",
                request.isForceRetrain(),
                request.getNote() != null ? request.getNote() : "(none)");

        List<ReviewSample> labelled = reviewSampleDao.findAllLabelled();
        long labelledCount = labelled.size();

        if (!request.isForceRetrain()
                && labelledCount < activeLearningProps.getMinTrainingSamples()) {
            throw new ResourceConflictException(
                    ErrorCode.INSUFFICIENT_TRAINING_DATA,
                    "Insufficient labelled samples for training. " +
                            "Required: " + activeLearningProps.getMinTrainingSamples() +
                            ", available: " + labelledCount +
                            ". Submit more labelled reviews or set forceRetrain=true.");
        }

        Collections.shuffle(labelled);

        int evalSize  = Math.max(
                MIN_EVAL_EXAMPLES,
                (int) (labelled.size() * EVAL_SPLIT_RATIO));
        int trainSize = labelled.size() - evalSize;

        if (trainSize < 1) {
            throw new ResourceConflictException(
                    ErrorCode.INSUFFICIENT_TRAINING_DATA,
                    "Not enough samples to produce both a training set and " +
                            "an evaluation set. Submit more labelled reviews.");
        }

        List<ReviewSample> trainSamples = labelled.subList(0, trainSize);
        List<ReviewSample> evalSamples  = labelled.subList(trainSize, labelled.size());

        log.info("Training split — train: {}, eval: {}", trainSize, evalSize);

        TextFeatureExtractor<Label> extractor = buildFeatureExtractor();

        MutableDataset<Label> trainDataset = buildDataset(trainSamples, extractor);
        MutableDataset<Label> evalDataset  = buildDataset(evalSamples,  extractor);

        MultinomialNaiveBayesTrainer trainer = new MultinomialNaiveBayesTrainer();
        Model<Label> newModel = trainer.train(trainDataset);

        log.info("Model trained on {} examples.", trainSize);

        double accuracy  = 0.0;
        double precision = 0.0;
        double recall    = 0.0;

        if (evalDataset.size() >= MIN_EVAL_EXAMPLES) {
            LabelEvaluator evaluator   = new LabelEvaluator();
            LabelEvaluation evaluation = evaluator.evaluate(newModel, evalDataset);
            accuracy  = evaluation.accuracy();
            precision = evaluation.macroAveragedPrecision();
            recall    = evaluation.macroAveragedRecall();
            log.info("Evaluation — accuracy: {:.4f}, precision: {:.4f}, recall: {:.4f}",
                    accuracy, precision, recall);
        } else {
            log.warn("Evaluation set too small ({} examples) — skipping evaluation.",
                    evalDataset.size());
        }

        classifier.updateModel(newModel);

        lastTrainedAt.set(LocalDateTime.now());
        samplesUsedInLastRun.set(labelledCount);
        accuracyLastRun.set(accuracy);
        precisionLastRun.set(precision);
        recallLastRun.set(recall);

        boolean savedToDisk = saveModelToDisk(newModel);

        long positiveCount = trainSamples.stream()
                .filter(s -> SentimentLabel.POSITIVE.name().equals(
                        s.getLabel().name()))
                .count();
        long negativeCount = trainSamples.size() - positiveCount;

        return TrainingResponse.builder()
                .trainedAt(LocalDateTime.now())
                .samplesUsed(labelledCount)
                .positiveCount(positiveCount)
                .negativeCount(negativeCount)
                .accuracyScore(accuracy)
                .precisionScore(precision)
                .recallScore(recall)
                .modelSavedToDisk(savedToDisk)
                .modelStoragePath(savedToDisk
                        ? activeLearningProps.getModelStoragePath()
                        : null)
                .note(request.getNote())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public ModelTrainer.LastRunStats getLastRunStats() {
        return new ModelTrainer.LastRunStats(
                lastTrainedAt.get(),
                samplesUsedInLastRun.get(),
                accuracyLastRun.get(),
                precisionLastRun.get(),
                recallLastRun.get());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Triggered automatically by Spring on {@link ApplicationReadyEvent},
     * after the full application context — including JPA repositories and
     * the datasource — is initialised and ready to serve requests.
     *
     * <p>Uses {@link EventListener} rather than {@code @PostConstruct} to
     * guarantee that the JPA layer is fully available when this method runs.
     */
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!activeLearningProps.isRetrainOnStartup()) {
            log.info("Startup model restoration disabled " +
                    "(active-learning.retrain-on-startup=false). " +
                    "Starting in untrained state.");
            return;
        }

        log.info("Startup model restoration sequence initiated.");

        Path modelPath = Paths.get(activeLearningProps.getModelStoragePath());

        if (Files.exists(modelPath)) {
            log.info("Saved model file found at: {}", modelPath);
            handleExistingModelFile(modelPath);
        } else {
            log.info("No saved model file found at: {}. Attempting initial training.",
                    modelPath);
            attemptInitialTraining();
        }
    }

    // -------------------------------------------------------------------------
    // Private — startup helpers
    // -------------------------------------------------------------------------

    /**
     * Handles the case where a serialised model file exists on disk at startup.
     *
     * <p>Checks whether the model is stale by comparing the number of labelled
     * samples currently in the database against the number of samples that
     * existed when the model was last saved. If the difference exceeds the
     * configured staleness threshold, the model is retrained. Otherwise the
     * saved model is loaded directly.
     *
     * @param modelPath the path to the existing model file
     */
    private void handleExistingModelFile(Path modelPath) {
        long currentLabelledCount = reviewSampleDao.countLabelled();
        long savedSampleCount     = readSavedSampleCount(modelPath);
        long newLabelsSinceSave   = currentLabelledCount - savedSampleCount;

        log.info("Labelled samples — current: {}, at last save: {}, new since save: {}",
                currentLabelledCount, savedSampleCount, newLabelsSinceSave);

        if (newLabelsSinceSave >= activeLearningProps.getStalenessThresholdSamples()) {
            log.info("Model is stale ({} new labels exceed threshold of {}). " +
                            "Retraining from database.",
                    newLabelsSinceSave,
                    activeLearningProps.getStalenessThresholdSamples());
            attemptInitialTraining();
        } else {
            log.info("Model is fresh. Loading from disk.");
            loadModelFromDisk(modelPath);
        }
    }

    /**
     * Attempts an initial training run at startup.
     *
     * <p>If insufficient labelled data exists, logs a warning and leaves the
     * application in an untrained state rather than throwing an exception —
     * a fresh deployment with no data is a normal and expected condition.
     */
    private void attemptInitialTraining() {
        long labelledCount = reviewSampleDao.countLabelled();

        if (labelledCount < activeLearningProps.getMinTrainingSamples()) {
            log.warn("Insufficient labelled samples for startup training " +
                            "({} available, {} required). " +
                            "Starting in untrained state — submit labelled reviews to enable classification.",
                    labelledCount,
                    activeLearningProps.getMinTrainingSamples());
            return;
        }

        try {
            TrainingRequest startupRequest = TrainingRequest.builder()
                    .forceRetrain(false)
                    .note("Automatic startup training.")
                    .build();
            train(startupRequest);
            log.info("Startup training completed successfully.");
        } catch (Exception ex) {
            log.error("Startup training failed unexpectedly: {}. " +
                    "Starting in untrained state.", ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private — model persistence
    // -------------------------------------------------------------------------

    /**
     * Serialises the trained model to the configured storage path using Java
     * object serialisation.
     *
     * <p>Creates parent directories if they do not exist. Logs a warning and
     * returns {@code false} on any I/O failure — the model remains available
     * in memory for classification even if persistence fails.
     *
     * @param model the trained model to serialise; must not be {@code null}
     * @return {@code true} if the model was saved successfully;
     *         {@code false} if an I/O error occurred
     */
    private boolean saveModelToDisk(Model<Label> model) {
        String path = activeLearningProps.getModelStoragePath();
        try {
            File modelFile = new File(path);
            if (modelFile.getParentFile() != null) {
                modelFile.getParentFile().mkdirs();
            }
            long labelledCount = reviewSampleDao.countLabelled();
            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(modelFile))) {
                oos.writeObject(model);
                oos.writeLong(labelledCount);
            }
            log.info("Model saved to disk at: {} (labelled sample count at save: {})",
                    path, labelledCount);
            return true;
        } catch (IOException ex) {
            log.warn("Failed to save model to disk at '{}': {}. " +
                            "Model is available in memory but will not survive a restart.",
                    path, ex.getMessage());
            return false;
        }
    }

    /**
     * Deserialises a previously saved model from disk and loads it into the
     * live {@link Classifier}.
     *
     * <p>Logs a warning and leaves the application in an untrained state if
     * deserialisation fails — for example, due to a corrupted file or a
     * serialisation version mismatch.
     *
     * @param modelPath the path to the serialised model file
     */
    @SuppressWarnings("unchecked")
    private void loadModelFromDisk(Path modelPath) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(modelPath.toFile()))) {
            Model<Label> loadedModel = (Model<Label>) ois.readObject();
            classifier.updateModel(loadedModel);
            log.info("Model loaded successfully from disk: {}", modelPath);
        } catch (IOException | ClassNotFoundException ex) {
            log.warn("Failed to load model from disk at '{}': {}. " +
                            "Attempting retraining from database.",
                    modelPath, ex.getMessage());
            attemptInitialTraining();
        }
    }

    /**
     * Reads the labelled sample count that was recorded when the model file
     * was last saved.
     *
     * <p>This count is written alongside the model during
     * {@link #saveModelToDisk(Model)} and is used at startup to determine
     * whether the saved model is stale.
     *
     * <p>Returns {@code 0} if the count cannot be read — for example, if the
     * file was saved by an older version that did not record this value —
     * which causes the model to be treated as maximally stale and triggers
     * a retrain.
     *
     * @param modelPath the path to the serialised model file
     * @return the labelled sample count at the time the model was saved,
     *         or {@code 0} if the count cannot be determined
     */
    @SuppressWarnings("unchecked")
    private long readSavedSampleCount(Path modelPath) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(modelPath.toFile()))) {
            ois.readObject();
            return ois.readLong();
        } catch (IOException | ClassNotFoundException ex) {
            log.warn("Could not read saved sample count from model file '{}': {}. " +
                    "Treating model as stale.", modelPath, ex.getMessage());
            return 0L;
        }
    }

    // -------------------------------------------------------------------------
    // Private — dataset construction
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the {@link TextFeatureExtractor} used to convert raw
     * review text into bag-of-words feature vectors.
     *
     * <p>Uses a {@link UniversalTokenizer} fed through a {@link BasicPipeline}
     * with unigram features (n=1). Must produce an identical feature space to
     * the extractor used in {@link ClassifierService} — if the two extractors
     * differ, predictions will be nonsensical.
     *
     * @return a configured {@link TextFeatureExtractorImpl}
     */
    private TextFeatureExtractor<Label> buildFeatureExtractor() {
        return new TextFeatureExtractorImpl<>(
                new BasicPipeline(new UniversalTokenizer(), 1));
    }

    /**
     * Converts a list of {@link ReviewSample} entities into a Tribuo
     * {@link MutableDataset} by extracting bag-of-words features from each
     * review text.
     *
     * <p>Each sample is converted into an {@link ArrayExample} pairing the
     * extracted feature vector with the sample's {@link Label}. Samples with
     * a {@code null} label are skipped with a warning — they should not appear
     * in a labelled dataset but are handled defensively.
     *
     * @param samples   the labelled review samples to convert
     * @param extractor the feature extractor to use for text conversion
     * @return a populated {@link MutableDataset} ready for training or
     *         evaluation
     */
    private MutableDataset<Label> buildDataset(
            List<ReviewSample> samples,
            TextFeatureExtractor<Label> extractor) {

        MutableDataset<Label> dataset = new MutableDataset<>(
                new SimpleDataSourceProvenance(
                        "ReviewSampleDataset", new LabelFactory()),
                new LabelFactory());

        for (ReviewSample sample : samples) {
            if (sample.getLabel() == null) {
                log.warn("Skipping unlabelled sample with ID: {}", sample.getId());
                continue;
            }
            Label label   = new Label(sample.getLabel().name());
            Example<Label> example = extractor.extract(label, sample.getReviewText());
            dataset.add(example);
        }

        return dataset;
    }
}