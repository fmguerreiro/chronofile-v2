#!/usr/bin/env python3
"""
TinyBERT training script for activity classification.
Fine-tunes TinyBERT for categorizing user activity text inputs.
"""

import os
import sys
import yaml
import pandas as pd
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import (
    AutoTokenizer, AutoModelForSequenceClassification,
    TrainingArguments, Trainer, EarlyStoppingCallback
)
from sklearn.metrics import accuracy_score, precision_recall_fscore_support, confusion_matrix
from sklearn.preprocessing import LabelEncoder
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import json
import logging
from typing import Dict, List, Tuple

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ActivityDataset(Dataset):
    """Dataset class for activity classification."""
    
    def __init__(self, texts: List[str], labels: List[str], confidences: List[float], 
                 tokenizer, max_length: int = 128):
        self.texts = texts
        self.labels = labels
        self.confidences = confidences
        self.tokenizer = tokenizer
        self.max_length = max_length
    
    def __len__(self):
        return len(self.texts)
    
    def __getitem__(self, idx):
        text = str(self.texts[idx])
        label = self.labels[idx]
        confidence = self.confidences[idx]
        
        # Tokenize text
        encoding = self.tokenizer(
            text,
            truncation=True,
            padding='max_length',
            max_length=self.max_length,
            return_tensors='pt'
        )
        
        return {
            'input_ids': encoding['input_ids'].flatten(),
            'attention_mask': encoding['attention_mask'].flatten(),
            'labels': torch.tensor(label, dtype=torch.long),
            'confidence': torch.tensor(confidence, dtype=torch.float)
        }

class ActivityClassificationTrainer:
    """Main trainer class for activity classification."""
    
    def __init__(self, config_path: str):
        self.config = self._load_config(config_path)
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        # Initialize components
        self.tokenizer = None
        self.model = None
        self.label_encoder = LabelEncoder()
        
    def _load_config(self, config_path: str) -> Dict:
        """Load training configuration."""
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    
    def load_data(self) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
        """Load training, validation, and test data."""
        train_df = pd.read_csv(self.config['data']['train_file'])
        val_df = pd.read_csv(self.config['data']['val_file'])
        test_df = pd.read_csv(self.config['data']['test_file'])
        
        logger.info(f"Loaded data - Train: {len(train_df)}, Val: {len(val_df)}, Test: {len(test_df)}")
        
        return train_df, val_df, test_df
    
    def prepare_model_and_tokenizer(self):
        """Initialize model and tokenizer."""
        model_name = self.config['model']['name']
        num_labels = self.config['model']['num_labels']
        
        logger.info(f"Loading model: {model_name}")
        
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForSequenceClassification.from_pretrained(
            model_name,
            num_labels=num_labels
        )
        
        # Add padding token if not present
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
            self.model.config.pad_token_id = self.tokenizer.eos_token_id
    
    def prepare_datasets(self, train_df: pd.DataFrame, val_df: pd.DataFrame, 
                        test_df: pd.DataFrame) -> Tuple[ActivityDataset, ActivityDataset, ActivityDataset]:
        """Prepare datasets for training."""
        text_col = self.config['data']['text_column']
        label_col = self.config['data']['label_column']
        conf_col = self.config['data']['confidence_column']
        
        # Fit label encoder on all categories
        all_labels = pd.concat([train_df[label_col], val_df[label_col], test_df[label_col]])
        self.label_encoder.fit(all_labels)
        
        # Encode labels
        train_labels = self.label_encoder.transform(train_df[label_col])
        val_labels = self.label_encoder.transform(val_df[label_col])
        test_labels = self.label_encoder.transform(test_df[label_col])
        
        # Create datasets
        max_length = self.config['model']['max_length']
        
        train_dataset = ActivityDataset(
            train_df[text_col].tolist(), train_labels, train_df[conf_col].tolist(),
            self.tokenizer, max_length
        )
        val_dataset = ActivityDataset(
            val_df[text_col].tolist(), val_labels, val_df[conf_col].tolist(),
            self.tokenizer, max_length
        )
        test_dataset = ActivityDataset(
            test_df[text_col].tolist(), test_labels, test_df[conf_col].tolist(),
            self.tokenizer, max_length
        )
        
        return train_dataset, val_dataset, test_dataset
    
    def compute_metrics(self, eval_pred):
        """Compute metrics for evaluation."""
        predictions, labels = eval_pred
        predictions = np.argmax(predictions, axis=1)
        
        # Calculate metrics
        accuracy = accuracy_score(labels, predictions)
        precision, recall, f1, _ = precision_recall_fscore_support(labels, predictions, average='weighted')
        
        # Calculate per-class F1 scores
        per_class_f1 = precision_recall_fscore_support(labels, predictions, average=None)[2]
        
        metrics = {
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1_weighted': f1,
        }
        
        # Add per-class F1 scores
        for i, class_name in enumerate(self.label_encoder.classes_):
            metrics[f'f1_{class_name}'] = per_class_f1[i]
        
        return metrics
    
    def train(self):
        """Main training loop."""
        # Load data
        train_df, val_df, test_df = self.load_data()
        
        # Prepare model
        self.prepare_model_and_tokenizer()
        
        # Prepare datasets
        train_dataset, val_dataset, test_dataset = self.prepare_datasets(train_df, val_df, test_df)
        
        # Set up training arguments
        training_args = TrainingArguments(
            output_dir=self.config['output']['output_dir'],
            logging_dir=self.config['output']['logging_dir'],
            
            num_train_epochs=self.config['training']['num_epochs'],
            learning_rate=self.config['training']['learning_rate'],
            per_device_train_batch_size=self.config['training']['batch_size'],
            per_device_eval_batch_size=self.config['training']['batch_size'],
            warmup_steps=self.config['training']['warmup_steps'],
            weight_decay=self.config['training']['weight_decay'],
            
            eval_strategy=self.config['validation']['eval_strategy'],
            eval_steps=self.config['validation']['eval_steps'],
            save_strategy=self.config['validation']['save_strategy'],
            save_steps=self.config['validation']['save_steps'],
            logging_steps=self.config['validation']['logging_steps'],
            
            load_best_model_at_end=self.config['validation']['load_best_model_at_end'],
            metric_for_best_model=self.config['validation']['metric_for_best_model'],
            greater_is_better=self.config['validation']['greater_is_better'],
            
            fp16=self.config['training']['fp16'],
            dataloader_num_workers=self.config['training']['dataloader_num_workers'],
            
            report_to="tensorboard",
            run_name="tinybert_activity_classification"
        )
        
        # Initialize trainer
        trainer = Trainer(
            model=self.model,
            args=training_args,
            train_dataset=train_dataset,
            eval_dataset=val_dataset,
            compute_metrics=self.compute_metrics,
            callbacks=[
                EarlyStoppingCallback(
                    early_stopping_patience=self.config['early_stopping']['patience'],
                    early_stopping_threshold=self.config['early_stopping']['min_delta']
                )
            ]
        )
        
        # Train model
        logger.info("Starting training...")
        trainer.train()
        
        # Save final model
        trainer.save_model()
        self.tokenizer.save_pretrained(self.config['output']['output_dir'])
        
        # Save label encoder
        label_encoder_path = Path(self.config['output']['output_dir']) / "label_encoder.json"
        with open(label_encoder_path, 'w') as f:
            json.dump({
                'classes': self.label_encoder.classes_.tolist(),
                'category_to_id': dict(zip(self.label_encoder.classes_, range(len(self.label_encoder.classes_))))
            }, f, indent=2)
        
        # Evaluate on test set
        logger.info("Evaluating on test set...")
        test_results = trainer.evaluate(test_dataset)
        
        # Generate detailed evaluation
        self._detailed_evaluation(trainer, test_dataset, test_df)
        
        logger.info("Training completed successfully!")
        return trainer
    
    def _detailed_evaluation(self, trainer, test_dataset: ActivityDataset, test_df: pd.DataFrame):
        """Generate detailed evaluation metrics and visualizations."""
        # Get predictions
        predictions = trainer.predict(test_dataset)
        y_pred = np.argmax(predictions.predictions, axis=1)
        y_true = predictions.label_ids
        
        # Generate classification report
        from sklearn.metrics import classification_report
        
        report = classification_report(
            y_true, y_pred,
            target_names=self.label_encoder.classes_,
            output_dict=True
        )
        
        # Save classification report
        output_dir = Path(self.config['output']['output_dir'])
        with open(output_dir / "classification_report.json", 'w') as f:
            json.dump(report, f, indent=2)
        
        # Generate confusion matrix
        cm = confusion_matrix(y_true, y_pred)
        
        plt.figure(figsize=(10, 8))
        sns.heatmap(
            cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=self.label_encoder.classes_,
            yticklabels=self.label_encoder.classes_
        )
        plt.title('Confusion Matrix')
        plt.ylabel('True Label')
        plt.xlabel('Predicted Label')
        plt.tight_layout()
        plt.savefig(output_dir / "confusion_matrix.png", dpi=300, bbox_inches='tight')
        plt.close()
        
        # Analyze errors
        errors_df = test_df.copy()
        errors_df['predicted'] = self.label_encoder.inverse_transform(y_pred)
        errors_df['correct'] = errors_df['category'] == errors_df['predicted']
        
        # Save error analysis
        error_examples = errors_df[~errors_df['correct']].head(50)
        error_examples.to_csv(output_dir / "error_analysis.csv", index=False)
        
        # Calculate confidence statistics
        confidences = torch.softmax(torch.tensor(predictions.predictions), dim=1).max(dim=1)[0].numpy()
        
        confidence_stats = {
            'mean_confidence': float(np.mean(confidences)),
            'std_confidence': float(np.std(confidences)),
            'min_confidence': float(np.min(confidences)),
            'max_confidence': float(np.max(confidences)),
            'confidence_by_correctness': {
                'correct_mean': float(np.mean(confidences[errors_df['correct']])),
                'incorrect_mean': float(np.mean(confidences[~errors_df['correct']]))
            }
        }
        
        with open(output_dir / "confidence_analysis.json", 'w') as f:
            json.dump(confidence_stats, f, indent=2)
        
        logger.info(f"Detailed evaluation saved to {output_dir}")


def main():
    """Main training script."""
    config_path = "config.yaml"
    
    if not os.path.exists(config_path):
        logger.error(f"Config file not found: {config_path}")
        sys.exit(1)
    
    trainer = ActivityClassificationTrainer(config_path)
    trainer.train()


if __name__ == "__main__":
    main()