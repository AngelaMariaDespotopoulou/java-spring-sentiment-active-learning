/**
 * Core business services for the Sentiment Active Learning application.
 *
 * <p>This package contains the three services that implement the active-learning
 * pipeline:
 *
 * <ul>
 *   <li>{@code ClassifierService} — wraps the Oracle Tribuo Naive Bayes model.
 *       Exposes classify operations and computes per-prediction confidence scores.
 *       When confidence falls below the configured uncertainty threshold, the
 *       prediction is flagged for oracle labelling.</li>
 *   <li>{@code TrainingService} — loads labelled samples from the DAO, builds
 *       the Tribuo {@code MutableDataset}, runs the training pipeline, evaluates
 *       accuracy/precision/recall on a hold-out set, and hands the trained model
 *       back to {@code ClassifierService}.</li>
 *   <li>{@code ActiveLearningService} — orchestrates the full cycle. Accumulates
 *       Claude-labelled samples, triggers retraining when the configured batch
 *       size is reached, and exposes cycle statistics for the management API.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.service.core;
