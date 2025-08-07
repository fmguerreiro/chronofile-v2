#!/usr/bin/env python3
"""
Model optimization and conversion script for mobile deployment.
Converts TinyBERT model to TensorFlow Lite with quantization.
"""

import os
import sys
import yaml
import torch
import tensorflow as tf
import numpy as np
import json
from pathlib import Path
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from transformers import TFAutoModelForSequenceClassification
import onnx
import onnxruntime as ort
from sklearn.metrics import accuracy_score
import pandas as pd
import logging
from typing import Dict, List, Tuple, Optional
import time

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class MobileModelConverter:
    """Converts and optimizes TinyBERT for mobile deployment."""
    
    def __init__(self, config_path: str, model_path: str):
        self.config = self._load_config(config_path)
        self.model_path = Path(model_path)
        self.output_dir = Path("../models/mobile")
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Model components
        self.tokenizer = None
        self.pytorch_model = None
        self.tf_model = None
        
    def _load_config(self, config_path: str) -> Dict:
        """Load configuration."""
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    
    def load_trained_model(self):
        """Load the trained PyTorch model."""
        logger.info(f"Loading trained model from {self.model_path}")
        
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_path)
        self.pytorch_model = AutoModelForSequenceClassification.from_pretrained(self.model_path)
        self.pytorch_model.eval()
        
        logger.info("Model loaded successfully")
    
    def convert_to_tensorflow(self) -> str:
        """Convert PyTorch model to TensorFlow."""
        logger.info("Converting to TensorFlow format...")
        
        tf_model_path = self.output_dir / "tensorflow_model"
        tf_model_path.mkdir(exist_ok=True)
        
        # Convert using transformers library
        self.tf_model = TFAutoModelForSequenceClassification.from_pretrained(
            self.model_path, 
            from_tf=False,
            cache_dir=None
        )
        
        # Save TensorFlow model
        self.tf_model.save_pretrained(tf_model_path)
        self.tokenizer.save_pretrained(tf_model_path)
        
        logger.info(f"TensorFlow model saved to {tf_model_path}")
        return str(tf_model_path)
    
    def convert_to_onnx(self) -> str:
        """Convert PyTorch model to ONNX format."""
        logger.info("Converting to ONNX format...")
        
        onnx_path = self.output_dir / "model.onnx"
        
        # Create dummy input
        max_length = self.config['model']['max_length']
        dummy_input = {
            'input_ids': torch.randint(0, 1000, (1, max_length), dtype=torch.long),
            'attention_mask': torch.ones(1, max_length, dtype=torch.long)
        }
        
        # Export to ONNX
        torch.onnx.export(
            self.pytorch_model,
            (dummy_input['input_ids'], dummy_input['attention_mask']),
            onnx_path,
            input_names=['input_ids', 'attention_mask'],
            output_names=['logits'],
            dynamic_axes={
                'input_ids': {0: 'batch_size', 1: 'sequence'},
                'attention_mask': {0: 'batch_size', 1: 'sequence'},
                'logits': {0: 'batch_size'}
            },
            opset_version=11,
            do_constant_folding=True
        )
        
        # Verify ONNX model
        onnx_model = onnx.load(str(onnx_path))
        onnx.checker.check_model(onnx_model)
        
        logger.info(f"ONNX model saved to {onnx_path}")
        return str(onnx_path)
    
    def quantize_onnx_model(self, onnx_path: str) -> str:
        """Quantize ONNX model for better performance."""
        try:
            from onnxruntime.quantization import quantize_dynamic, QuantType
            
            logger.info("Quantizing ONNX model...")
            
            quantized_path = self.output_dir / "model_quantized.onnx"
            
            quantize_dynamic(
                onnx_path,
                str(quantized_path),
                weight_type=QuantType.QInt8
            )
            
            logger.info(f"Quantized ONNX model saved to {quantized_path}")
            return str(quantized_path)
            
        except ImportError:
            logger.warning("ONNX quantization not available, skipping...")
            return onnx_path
    
    def convert_to_tflite(self, tf_model_path: str) -> str:
        """Convert TensorFlow model to TensorFlow Lite with quantization."""
        logger.info("Converting to TensorFlow Lite...")
        
        # Load TensorFlow model
        model = tf.saved_model.load(tf_model_path)
        
        # Create converter
        converter = tf.lite.TFLiteConverter.from_saved_model(tf_model_path)
        
        # Apply optimizations
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # Enable dynamic range quantization
        converter.representative_dataset = self._representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.int32
        converter.inference_output_type = tf.float32
        
        try:
            # Convert with quantization
            tflite_model = converter.convert()
            quantized = True
            
        except Exception as e:
            logger.warning(f"Quantized conversion failed: {e}")
            logger.info("Falling back to float32 conversion...")
            
            # Fallback to float32
            converter = tf.lite.TFLiteConverter.from_saved_model(tf_model_path)
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            tflite_model = converter.convert()
            quantized = False
        
        # Save TFLite model
        tflite_path = self.output_dir / ("model_quantized.tflite" if quantized else "model.tflite")
        with open(tflite_path, 'wb') as f:
            f.write(tflite_model)
        
        model_size_mb = len(tflite_model) / (1024 * 1024)
        logger.info(f"TensorFlow Lite model saved to {tflite_path} ({model_size_mb:.1f}MB)")
        
        return str(tflite_path)
    
    def _representative_dataset(self):
        """Generate representative dataset for quantization."""
        # Load some sample data for quantization calibration
        try:
            test_df = pd.read_csv(self.config['data']['test_file'])
            sample_texts = test_df['user_input'].head(100).tolist()
        except:
            # Fallback to synthetic data
            sample_texts = [
                "morning run", "work meeting", "lunch break", "evening yoga",
                "coding session", "family dinner", "gym workout", "study time"
            ] * 12  # 96 samples
        
        max_length = self.config['model']['max_length']
        
        for text in sample_texts:
            # Tokenize
            encoding = self.tokenizer(
                text,
                truncation=True,
                padding='max_length',
                max_length=max_length,
                return_tensors='tf'
            )
            
            # Yield as generator
            yield [
                encoding['input_ids'].numpy().astype(np.int32),
                encoding['attention_mask'].numpy().astype(np.int32)
            ]
    
    def benchmark_models(self, test_data_path: Optional[str] = None) -> Dict:
        """Benchmark different model formats for latency and accuracy."""
        logger.info("Benchmarking model performance...")
        
        results = {}
        
        # Load test data
        if test_data_path:
            test_df = pd.read_csv(test_data_path)
            test_texts = test_df['user_input'].head(100).tolist()  # Sample for benchmarking
        else:
            test_texts = [
                "morning run", "team meeting", "lunch with friends", "evening workout",
                "coding project", "family time", "gym session", "study break"
            ] * 12  # 96 test samples
        
        # Benchmark PyTorch model
        results['pytorch'] = self._benchmark_pytorch(test_texts)
        
        # Benchmark ONNX model
        onnx_path = self.output_dir / "model.onnx"
        if onnx_path.exists():
            results['onnx'] = self._benchmark_onnx(str(onnx_path), test_texts)
        
        # Benchmark quantized ONNX model
        quantized_onnx_path = self.output_dir / "model_quantized.onnx"
        if quantized_onnx_path.exists():
            results['onnx_quantized'] = self._benchmark_onnx(str(quantized_onnx_path), test_texts)
        
        # Benchmark TFLite model
        tflite_path = self.output_dir / "model_quantized.tflite"
        if not tflite_path.exists():
            tflite_path = self.output_dir / "model.tflite"
        
        if tflite_path.exists():
            results['tflite'] = self._benchmark_tflite(str(tflite_path), test_texts)
        
        # Save benchmark results
        with open(self.output_dir / "benchmark_results.json", 'w') as f:
            json.dump(results, f, indent=2)
        
        # Print summary
        self._print_benchmark_summary(results)
        
        return results
    
    def _benchmark_pytorch(self, test_texts: List[str]) -> Dict:
        """Benchmark PyTorch model."""
        logger.info("Benchmarking PyTorch model...")
        
        max_length = self.config['model']['max_length']
        times = []
        
        with torch.no_grad():
            for text in test_texts:
                start_time = time.time()
                
                # Tokenize
                encoding = self.tokenizer(
                    text,
                    truncation=True,
                    padding='max_length',
                    max_length=max_length,
                    return_tensors='pt'
                )
                
                # Inference
                outputs = self.pytorch_model(**encoding)
                predictions = torch.softmax(outputs.logits, dim=1)
                
                end_time = time.time()
                times.append((end_time - start_time) * 1000)  # Convert to ms
        
        return {
            'mean_latency_ms': np.mean(times),
            'std_latency_ms': np.std(times),
            'p95_latency_ms': np.percentile(times, 95),
            'model_size_mb': self._get_pytorch_model_size()
        }
    
    def _benchmark_onnx(self, onnx_path: str, test_texts: List[str]) -> Dict:
        """Benchmark ONNX model."""
        logger.info(f"Benchmarking ONNX model: {Path(onnx_path).name}")
        
        # Load ONNX model
        session = ort.InferenceSession(onnx_path)
        
        max_length = self.config['model']['max_length']
        times = []
        
        for text in test_texts:
            start_time = time.time()
            
            # Tokenize
            encoding = self.tokenizer(
                text,
                truncation=True,
                padding='max_length',
                max_length=max_length,
                return_tensors='np'
            )
            
            # Inference
            outputs = session.run(
                None,
                {
                    'input_ids': encoding['input_ids'].astype(np.int64),
                    'attention_mask': encoding['attention_mask'].astype(np.int64)
                }
            )
            
            end_time = time.time()
            times.append((end_time - start_time) * 1000)  # Convert to ms
        
        model_size = os.path.getsize(onnx_path) / (1024 * 1024)  # MB
        
        return {
            'mean_latency_ms': np.mean(times),
            'std_latency_ms': np.std(times),
            'p95_latency_ms': np.percentile(times, 95),
            'model_size_mb': model_size
        }
    
    def _benchmark_tflite(self, tflite_path: str, test_texts: List[str]) -> Dict:
        """Benchmark TensorFlow Lite model."""
        logger.info(f"Benchmarking TFLite model: {Path(tflite_path).name}")
        
        # Load TFLite model
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        max_length = self.config['model']['max_length']
        times = []
        
        for text in test_texts:
            start_time = time.time()
            
            # Tokenize
            encoding = self.tokenizer(
                text,
                truncation=True,
                padding='max_length',
                max_length=max_length,
                return_tensors='np'
            )
            
            # Set input tensors
            interpreter.set_tensor(input_details[0]['index'], encoding['input_ids'].astype(np.int32))
            interpreter.set_tensor(input_details[1]['index'], encoding['attention_mask'].astype(np.int32))
            
            # Inference
            interpreter.invoke()
            
            # Get output
            outputs = interpreter.get_tensor(output_details[0]['index'])
            
            end_time = time.time()
            times.append((end_time - start_time) * 1000)  # Convert to ms
        
        model_size = os.path.getsize(tflite_path) / (1024 * 1024)  # MB
        
        return {
            'mean_latency_ms': np.mean(times),
            'std_latency_ms': np.std(times),
            'p95_latency_ms': np.percentile(times, 95),
            'model_size_mb': model_size
        }
    
    def _get_pytorch_model_size(self) -> float:
        """Estimate PyTorch model size in MB."""
        param_size = 0
        buffer_size = 0
        
        for param in self.pytorch_model.parameters():
            param_size += param.nelement() * param.element_size()
        
        for buffer in self.pytorch_model.buffers():
            buffer_size += buffer.nelement() * buffer.element_size()
        
        size_all_mb = (param_size + buffer_size) / 1024**2
        return size_all_mb
    
    def _print_benchmark_summary(self, results: Dict):
        """Print benchmark summary."""
        print("\n" + "="*60)
        print("MODEL BENCHMARK SUMMARY")
        print("="*60)
        
        for model_name, metrics in results.items():
            print(f"\n{model_name.upper()}")
            print(f"  Latency: {metrics['mean_latency_ms']:.1f}ms ± {metrics['std_latency_ms']:.1f}ms")
            print(f"  P95 Latency: {metrics['p95_latency_ms']:.1f}ms")
            print(f"  Model Size: {metrics['model_size_mb']:.1f}MB")
        
        print("\n" + "="*60)
    
    def create_android_assets(self):
        """Create Android-ready assets."""
        logger.info("Creating Android assets...")
        
        android_dir = Path("../android_integration/model_assets")
        android_dir.mkdir(parents=True, exist_ok=True)
        
        # Copy TFLite model
        tflite_files = list(self.output_dir.glob("*.tflite"))
        if tflite_files:
            best_tflite = min(tflite_files, key=lambda x: os.path.getsize(x))
            import shutil
            shutil.copy2(best_tflite, android_dir / "activity_classifier.tflite")
            logger.info(f"Copied {best_tflite.name} to Android assets")
        
        # Create tokenizer assets
        tokenizer_config = {
            'vocab_size': len(self.tokenizer.vocab),
            'max_length': self.config['model']['max_length'],
            'pad_token': self.tokenizer.pad_token,
            'pad_token_id': self.tokenizer.pad_token_id,
            'unk_token': self.tokenizer.unk_token,
            'unk_token_id': self.tokenizer.unk_token_id
        }
        
        with open(android_dir / "tokenizer_config.json", 'w') as f:
            json.dump(tokenizer_config, f, indent=2)
        
        # Save vocabulary
        vocab = dict(sorted(self.tokenizer.vocab.items(), key=lambda x: x[1]))
        with open(android_dir / "vocab.json", 'w') as f:
            json.dump(vocab, f, indent=2)
        
        # Copy label encoder
        label_encoder_path = self.model_path / "label_encoder.json"
        if label_encoder_path.exists():
            shutil.copy2(label_encoder_path, android_dir / "label_encoder.json")
        
        logger.info(f"Android assets created in {android_dir}")
    
    def convert_all(self):
        """Run complete conversion pipeline."""
        logger.info("Starting complete model conversion pipeline...")
        
        # Load trained model
        self.load_trained_model()
        
        # Convert to different formats
        tf_model_path = self.convert_to_tensorflow()
        onnx_path = self.convert_to_onnx()
        quantized_onnx_path = self.quantize_onnx_model(onnx_path)
        tflite_path = self.convert_to_tflite(tf_model_path)
        
        # Benchmark all models
        self.benchmark_models(self.config['data']['test_file'])
        
        # Create Android assets
        self.create_android_assets()
        
        logger.info("Model conversion pipeline completed!")


def main():
    """Main conversion script."""
    if len(sys.argv) != 3:
        print("Usage: python convert_to_mobile.py <config_path> <model_path>")
        sys.exit(1)
    
    config_path = sys.argv[1]
    model_path = sys.argv[2]
    
    if not os.path.exists(config_path):
        logger.error(f"Config file not found: {config_path}")
        sys.exit(1)
    
    if not os.path.exists(model_path):
        logger.error(f"Model path not found: {model_path}")
        sys.exit(1)
    
    converter = MobileModelConverter(config_path, model_path)
    converter.convert_all()


if __name__ == "__main__":
    main()