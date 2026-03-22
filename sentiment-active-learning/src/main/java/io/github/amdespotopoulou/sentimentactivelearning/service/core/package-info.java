/**
 * Core business services for the Sentiment Active Learning application.
 *
 * <p>This package contains the three interface/implementation pairs that implement
 * the active-learning pipeline. Each service is declared as an interface first
 * (for testability and replaceability) with a single production implementation:
 *
 * <ul>
 *   <li>{@code Classifier} / {@code ClassifierService} — holds the trained
 *       Tribuo Naive Bayes model in memory. Classifies review texts, computes
 *       per-prediction confidence scores, and flags uncertain predictions for
 *       oracle labelling. Supports atomic hot-swapping of the model after each
 *       retraining run.</li>
 *   <li>{@code ModelTrainer} / {@code TrainingService} — fetches labelled
 *       samples from the DAO, builds the Tribuo {@code MutableDataset}, trains
 *       the Naive Bayes model, evaluates accuracy/precision/recall on a hold-out
 *       set, hot-swaps the live model, and persists the result to disk. Also
 *       handles the startup model restoration sequence.</li>
 *   <li>{@code ActiveLearner} / {@code ActiveLearningService} — orchestrates
 *       the full active-learning cycle. Receives review submissions, drives
 *       classification, consults the Claude oracle for uncertain predictions,
 *       accumulates Claude-labelled samples, triggers retraining when the
 *       configured batch size is reached, and exposes cycle statistics for
 *       the management API.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.service.core;