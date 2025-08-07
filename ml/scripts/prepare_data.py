#!/usr/bin/env python3
"""
Data preparation pipeline for TinyBERT activity classification.
Generates training data from existing keywords and synthetic augmentation.
"""

import pandas as pd
import numpy as np
import json
import random
from typing import List, Dict, Tuple
from pathlib import Path
import re
from dataclasses import dataclass
from collections import defaultdict

@dataclass
class CategoryData:
    name: str
    icon_res: str
    keywords: List[str]
    templates: List[str]
    character_features: List[str]

class ActivityDataGenerator:
    def __init__(self):
        self.categories = self._load_categories()
        self.activity_templates = self._load_templates()
        self.multilingual_terms = self._load_multilingual_terms()
        
    def _load_categories(self) -> Dict[str, CategoryData]:
        """Load category data from the existing ActivityClassifier structure."""
        return {
            "exercise": CategoryData(
                name="exercise",
                icon_res="ic_run",
                keywords=[
                    "exercise", "workout", "gym", "fitness", "sport", "training", "run", 
                    "walk", "bike", "swim", "yoga", "stretch", "jog", "hike", "dance", 
                    "pilates", "cardio", "weights", "boxing", "martial", "tennis", 
                    "football", "basketball", "soccer", "baseball", "golf", "skiing", 
                    "climbing", "rowing", "cycling"
                ],
                templates=[
                    "morning {activity}", "{activity} session", "go {activity}", 
                    "quick {activity}", "{activity} at the gym", "outdoor {activity}",
                    "{duration} minute {activity}", "{activity} with friends",
                    "evening {activity}", "{activity} routine"
                ],
                character_features=["movement", "physical", "active", "athletic", "outdoor"]
            ),
            "work": CategoryData(
                name="work",
                icon_res="ic_briefcase_line",
                keywords=[
                    "work", "job", "office", "meeting", "project", "task", "business", 
                    "career", "employment", "professional", "client", "deadline", 
                    "presentation", "email", "call", "conference", "coding", 
                    "programming", "development", "design", "writing", "analysis", 
                    "research", "planning", "strategy", "management", "admin", 
                    "paperwork", "document", "report", "contract", "agreement", 
                    "legal", "negotiation", "deal", "proposal", "review", "audit"
                ],
                templates=[
                    "team {activity}", "{activity} meeting", "client {activity}",
                    "{activity} call", "urgent {activity}", "daily {activity}",
                    "{activity} session", "project {activity}", "office {activity}",
                    "{activity} review"
                ],
                character_features=["professional", "productive", "business", "technical", "corporate"]
            ),
            "food": CategoryData(
                name="food",
                icon_res="ic_utensils_line",
                keywords=[
                    "food", "eat", "meal", "lunch", "dinner", "breakfast", "snack", 
                    "cooking", "kitchen", "restaurant", "cafe", "coffee", "tea", 
                    "drink", "water", "juice", "smoothie", "pizza", "burger", 
                    "salad", "sandwich", "soup", "pasta", "rice", "bread", "fruit", 
                    "vegetable", "meat", "fish", "dessert", "cake", "chocolate"
                ],
                templates=[
                    "having {activity}", "{activity} break", "quick {activity}",
                    "healthy {activity}", "homemade {activity}", "{activity} prep",
                    "grabbing {activity}", "{activity} with family", "takeout {activity}",
                    "morning {activity}"
                ],
                character_features=["consumption", "culinary", "nutrition", "dining", "beverage"]
            ),
            "sleep": CategoryData(
                name="sleep",
                icon_res="ic_moon_line",
                keywords=[
                    "sleep", "rest", "nap", "bed", "tired", "relax", "night", "dream", 
                    "pillow", "bedroom", "wake", "alarm", "morning", "evening", 
                    "bedtime", "drowsy", "exhausted", "recharge", "recover", "peaceful", 
                    "quiet", "dark"
                ],
                templates=[
                    "time to {activity}", "getting some {activity}", "{activity} break",
                    "quick {activity}", "afternoon {activity}", "much needed {activity}",
                    "finally {activity}", "peaceful {activity}", "deep {activity}",
                    "{duration} hours of {activity}"
                ],
                character_features=["restful", "recovery", "nighttime", "peaceful", "relaxation"]
            ),
            "social": CategoryData(
                name="social",
                icon_res="ic_people",
                keywords=[
                    "social", "friends", "family", "people", "party", "gathering", 
                    "visit", "chat", "talk", "date", "hangout", "dinner", "lunch", 
                    "coffee", "drinks", "celebration", "wedding", "birthday", "event", 
                    "community", "group", "team", "colleague", "neighbor", "conversation", 
                    "relationship"
                ],
                templates=[
                    "hanging out {activity}", "{activity} with friends", "family {activity}",
                    "group {activity}", "social {activity}", "{activity} gathering",
                    "fun {activity}", "weekend {activity}", "special {activity}",
                    "catching up {activity}"
                ],
                character_features=["interpersonal", "community", "relationship", "gathering", "interactive"]
            ),
            "learning": CategoryData(
                name="learning",
                icon_res="ic_note",
                keywords=[
                    "study", "learn", "read", "book", "education", "school", "university", 
                    "course", "research", "homework", "assignment", "exam", "test", 
                    "lecture", "class", "lesson", "tutorial", "workshop", "seminar", 
                    "training", "skill", "knowledge", "practice", "review", "analyze"
                ],
                templates=[
                    "{activity} session", "online {activity}", "intensive {activity}",
                    "{activity} group", "self {activity}", "focused {activity}",
                    "{activity} materials", "{activity} notes", "exam {activity}",
                    "skill {activity}"
                ],
                character_features=["educational", "intellectual", "academic", "cognitive", "informational"]
            ),
            "entertainment": CategoryData(
                name="entertainment",
                icon_res="ic_tv",
                keywords=[
                    "entertainment", "tv", "movie", "film", "show", "video", "game", 
                    "play", "fun", "leisure", "music", "listen", "watch", "streaming", 
                    "netflix", "youtube", "podcast", "radio", "concert", "theater", 
                    "comedy", "drama", "action", "adventure", "hobby", "relaxation"
                ],
                templates=[
                    "watching {activity}", "playing {activity}", "enjoying {activity}",
                    "binge {activity}", "new {activity}", "favorite {activity}",
                    "relaxing {activity}", "weekend {activity}", "evening {activity}",
                    "fun {activity}"
                ],
                character_features=["recreational", "leisure", "enjoyment", "media", "amusing"]
            ),
            "health": CategoryData(
                name="health",
                icon_res="ic_health",
                keywords=[
                    "health", "medical", "doctor", "hospital", "medicine", "therapy", 
                    "wellness", "checkup", "appointment", "dentist", "pharmacy", 
                    "treatment", "medication", "surgery", "clinic", "nurse", "patient", 
                    "symptoms", "diagnosis", "recovery", "healing", "prevention"
                ],
                templates=[
                    "{activity} appointment", "routine {activity}", "follow-up {activity}",
                    "urgent {activity}", "preventive {activity}", "{activity} visit",
                    "annual {activity}", "health {activity}", "medical {activity}",
                    "{activity} consultation"
                ],
                character_features=["medical", "wellness", "therapeutic", "clinical", "healthcare"]
            ),
            "travel": CategoryData(
                name="travel",
                icon_res="ic_car",
                keywords=[
                    "travel", "trip", "journey", "drive", "flight", "train", "bus", 
                    "commute", "transport", "vacation", "holiday", "airport", "station", 
                    "road", "highway", "traffic", "destination", "explore", "adventure", 
                    "sightseeing", "tourism", "hotel", "booking"
                ],
                templates=[
                    "planning {activity}", "booking {activity}", "morning {activity}",
                    "long {activity}", "quick {activity}", "business {activity}",
                    "vacation {activity}", "weekend {activity}", "road {activity}",
                    "{activity} adventure"
                ],
                character_features=["movement", "transportation", "journey", "exploration", "mobility"]
            )
        }
    
    def _load_templates(self) -> Dict[str, List[str]]:
        """Load generic activity templates for data augmentation."""
        return {
            "time_prefixes": [
                "morning", "afternoon", "evening", "late night", "early morning",
                "lunch time", "after work", "weekend", "today", "quick"
            ],
            "duration_modifiers": [
                "15 minute", "30 minute", "1 hour", "2 hour", "short", "long", "extended", "brief"
            ],
            "intensity_modifiers": [
                "intense", "light", "heavy", "gentle", "vigorous", "relaxing", "challenging", "easy"
            ],
            "location_modifiers": [
                "at home", "at the gym", "outside", "in the office", "at the park",
                "downtown", "nearby", "local"
            ],
            "social_modifiers": [
                "with friends", "solo", "with family", "with team", "group", "alone"
            ]
        }
    
    def _load_multilingual_terms(self) -> Dict[str, Dict[str, List[str]]]:
        """Load multilingual variations for key terms."""
        return {
            "exercise": {
                "spanish": ["ejercicio", "gimnasio", "deportes", "correr", "caminar", "nadar"],
                "french": ["exercice", "sport", "course", "marche", "natation"],
                "german": ["übung", "sport", "laufen", "schwimmen"],
                "portuguese": ["exercício", "academia", "esporte", "corrida", "caminhada"]
            },
            "work": {
                "spanish": ["trabajo", "oficina", "reunión", "proyecto", "negocio"],
                "french": ["travail", "bureau", "réunion", "projet", "affaires"],
                "german": ["arbeit", "büro", "meeting", "projekt", "geschäft"],
                "portuguese": ["trabalho", "escritório", "reunião", "projeto", "negócio"]
            },
            "food": {
                "spanish": ["comida", "comer", "almuerzo", "cena", "desayuno", "cocinar"],
                "french": ["nourriture", "manger", "repas", "déjeuner", "dîner"],
                "german": ["essen", "mahlzeit", "mittagessen", "abendessen", "frühstück"],
                "portuguese": ["comida", "comer", "almoço", "jantar", "café da manhã"]
            }
        }
    
    def generate_base_examples(self) -> List[Dict]:
        """Generate base examples from existing keywords."""
        examples = []
        
        for category_name, category_data in self.categories.items():
            # Add direct keyword examples
            for keyword in category_data.keywords:
                examples.append({
                    "user_input": keyword,
                    "icon_label": category_data.icon_res,
                    "category": category_name,
                    "confidence_score": 1.0,
                    "source": "direct_keyword"
                })
                
                # Add simple variations
                examples.extend([
                    {
                        "user_input": f"go {keyword}",
                        "icon_label": category_data.icon_res,
                        "category": category_name,
                        "confidence_score": 0.9,
                        "source": "keyword_variation"
                    },
                    {
                        "user_input": f"{keyword} session",
                        "icon_label": category_data.icon_res,
                        "category": category_name,
                        "confidence_score": 0.9,
                        "source": "keyword_variation"
                    }
                ])
        
        return examples
    
    def generate_template_examples(self) -> List[Dict]:
        """Generate examples using activity templates."""
        examples = []
        
        for category_name, category_data in self.categories.items():
            for template in category_data.templates:
                # Use top keywords for this category
                top_keywords = category_data.keywords[:10]
                
                for keyword in top_keywords:
                    try:
                        if "{duration}" in template:
                            duration = random.choice(["15", "30", "45", "60"])
                            activity_text = template.format(duration=duration, activity=keyword)
                        elif "{activity}" in template:
                            activity_text = template.format(activity=keyword)
                        else:
                            activity_text = f"{template} {keyword}"
                    except KeyError:
                        # Handle templates with missing placeholders
                        activity_text = f"{keyword} {template}".replace("{activity}", "").replace("{duration}", "")
                    
                    examples.append({
                        "user_input": activity_text,
                        "icon_label": category_data.icon_res,
                        "category": category_name,
                        "confidence_score": 0.8,
                        "source": "template_generated"
                    })
        
        return examples
    
    def generate_contextual_examples(self) -> List[Dict]:
        """Generate contextual examples with modifiers."""
        examples = []
        templates = self.activity_templates
        
        for category_name, category_data in self.categories.items():
            # Use subset of keywords to avoid explosion
            keywords = category_data.keywords[:8]
            
            for keyword in keywords:
                # Time-based contexts
                for time_prefix in templates["time_prefixes"][:5]:
                    examples.append({
                        "user_input": f"{time_prefix} {keyword}",
                        "icon_label": category_data.icon_res,
                        "category": category_name,
                        "confidence_score": 0.7,
                        "source": "contextual"
                    })
                
                # Duration contexts
                for duration in templates["duration_modifiers"][:3]:
                    examples.append({
                        "user_input": f"{duration} {keyword}",
                        "icon_label": category_data.icon_res,
                        "category": category_name,
                        "confidence_score": 0.7,
                        "source": "contextual"
                    })
        
        return examples
    
    def generate_multilingual_examples(self) -> List[Dict]:
        """Generate multilingual examples."""
        examples = []
        
        for category_name in ["exercise", "work", "food"]:  # Subset for now
            category_data = self.categories[category_name]
            
            if category_name in self.multilingual_terms:
                for lang, terms in self.multilingual_terms[category_name].items():
                    for term in terms[:3]:  # Limit per language
                        examples.append({
                            "user_input": term,
                            "icon_label": category_data.icon_res,
                            "category": category_name,
                            "confidence_score": 0.9,
                            "source": f"multilingual_{lang}"
                        })
        
        return examples
    
    def generate_realistic_examples(self) -> List[Dict]:
        """Generate realistic user input examples."""
        realistic_patterns = [
            # Exercise patterns
            ("morning run 5km", "exercise", "ic_run", 1.0),
            ("gym workout legs", "exercise", "ic_run", 0.9),
            ("walk the dog", "exercise", "ic_run", 0.8),
            ("yoga class", "exercise", "ic_run", 0.9),
            ("bike to work", "exercise", "ic_run", 0.7),
            
            # Work patterns
            ("standup meeting", "work", "ic_briefcase_line", 0.9),
            ("code review", "work", "ic_briefcase_line", 0.9),
            ("client call", "work", "ic_briefcase_line", 1.0),
            ("finish presentation", "work", "ic_briefcase_line", 0.8),
            ("team retrospective", "work", "ic_briefcase_line", 0.8),
            
            # Food patterns
            ("lunch with sarah", "social", "ic_people", 0.6),  # Ambiguous case
            ("grab coffee", "food", "ic_utensils_line", 0.8),
            ("meal prep sunday", "food", "ic_utensils_line", 0.9),
            ("dinner at home", "food", "ic_utensils_line", 1.0),
            
            # Social patterns
            ("birthday party", "social", "ic_people", 1.0),
            ("drinks with colleagues", "social", "ic_people", 0.9),
            ("family dinner", "social", "ic_people", 0.8),
            ("wedding reception", "social", "ic_people", 1.0),
            
            # Mixed/ambiguous patterns (important for training)
            ("business lunch", "work", "ic_briefcase_line", 0.6),
            ("work from cafe", "work", "ic_briefcase_line", 0.7),
            ("study group", "learning", "ic_note", 0.9),
            ("research project", "learning", "ic_note", 0.8),
        ]
        
        examples = []
        for text, category, icon, confidence in realistic_patterns:
            examples.append({
                "user_input": text,
                "icon_label": icon,
                "category": category,
                "confidence_score": confidence,
                "source": "realistic_pattern"
            })
        
        return examples
    
    def add_noise_and_variations(self, examples: List[Dict]) -> List[Dict]:
        """Add natural variations and noise to examples."""
        variations = []
        
        for example in examples[:500]:  # Apply to subset to control size
            text = example["user_input"]
            
            # Add typos (5% chance)
            if random.random() < 0.05:
                typo_text = self._add_typo(text)
                variations.append({
                    **example,
                    "user_input": typo_text,
                    "confidence_score": example["confidence_score"] * 0.9,
                    "source": f"{example['source']}_typo"
                })
            
            # Add punctuation variations
            if random.random() < 0.1:
                punct_text = text + random.choice(["!", ".", "?", "..."])
                variations.append({
                    **example,
                    "user_input": punct_text,
                    "confidence_score": example["confidence_score"],
                    "source": f"{example['source']}_punct"
                })
        
        return variations
    
    def _add_typo(self, text: str) -> str:
        """Add a simple typo to text."""
        if len(text) < 3:
            return text
        
        # Simple character substitution
        pos = random.randint(1, len(text) - 2)
        chars = list(text)
        chars[pos] = random.choice('abcdefghijklmnopqrstuvwxyz')
        return ''.join(chars)
    
    def balance_dataset(self, examples: List[Dict]) -> List[Dict]:
        """Balance the dataset across categories."""
        category_counts = defaultdict(list)
        
        for example in examples:
            category_counts[example["category"]].append(example)
        
        # Find minimum and maximum counts
        counts = [len(examples) for examples in category_counts.values()]
        min_count = min(counts)
        max_count = max(counts)
        
        # Target count (between min and reasonable upper bound)
        target_count = min(max_count, min_count * 2, 1000)
        
        balanced_examples = []
        for category, category_examples in category_counts.items():
            if len(category_examples) <= target_count:
                balanced_examples.extend(category_examples)
            else:
                # Randomly sample to target count
                sampled = random.sample(category_examples, target_count)
                balanced_examples.extend(sampled)
        
        return balanced_examples
    
    def generate_all_data(self, target_total: int = 10000) -> pd.DataFrame:
        """Generate complete training dataset."""
        print("Generating base examples...")
        examples = self.generate_base_examples()
        
        print("Generating template examples...")
        examples.extend(self.generate_template_examples())
        
        print("Generating contextual examples...")
        examples.extend(self.generate_contextual_examples())
        
        print("Generating multilingual examples...")
        examples.extend(self.generate_multilingual_examples())
        
        print("Generating realistic examples...")
        examples.extend(self.generate_realistic_examples())
        
        print("Adding variations and noise...")
        examples.extend(self.add_noise_and_variations(examples))
        
        print(f"Generated {len(examples)} examples before balancing")
        
        print("Balancing dataset...")
        examples = self.balance_dataset(examples)
        
        # Convert to DataFrame
        df = pd.DataFrame(examples)
        
        # Remove duplicates
        df = df.drop_duplicates(subset=['user_input', 'category'])
        
        print(f"Final dataset: {len(df)} examples")
        print("\nCategory distribution:")
        print(df['category'].value_counts())
        
        return df


def split_data(df: pd.DataFrame, train_ratio: float = 0.7, val_ratio: float = 0.15) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """Split data into train/validation/test sets stratified by category."""
    
    train_dfs, val_dfs, test_dfs = [], [], []
    
    for category in df['category'].unique():
        category_df = df[df['category'] == category].copy()
        category_df = category_df.sample(frac=1, random_state=42).reset_index(drop=True)  # Shuffle
        
        n = len(category_df)
        train_end = int(n * train_ratio)
        val_end = int(n * (train_ratio + val_ratio))
        
        train_dfs.append(category_df[:train_end])
        val_dfs.append(category_df[train_end:val_end])
        test_dfs.append(category_df[val_end:])
    
    train_df = pd.concat(train_dfs, ignore_index=True).sample(frac=1, random_state=42)
    val_df = pd.concat(val_dfs, ignore_index=True).sample(frac=1, random_state=42)
    test_df = pd.concat(test_dfs, ignore_index=True).sample(frac=1, random_state=42)
    
    return train_df, val_df, test_df


def main():
    """Main data preparation pipeline."""
    print("Starting data preparation pipeline...")
    
    # Create output directory
    output_dir = Path("../data")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Generate data
    generator = ActivityDataGenerator()
    df = generator.generate_all_data()
    
    # Split data
    train_df, val_df, test_df = split_data(df)
    
    # Save datasets
    train_df.to_csv(output_dir / "training_data.csv", index=False)
    val_df.to_csv(output_dir / "validation_data.csv", index=False)
    test_df.to_csv(output_dir / "test_data.csv", index=False)
    
    print(f"\nDatasets saved:")
    print(f"Training: {len(train_df)} examples")
    print(f"Validation: {len(val_df)} examples") 
    print(f"Test: {len(test_df)} examples")
    
    # Save category mapping
    category_mapping = {
        category: data.icon_res 
        for category, data in generator.categories.items()
    }
    
    with open(output_dir / "category_mapping.json", "w") as f:
        json.dump(category_mapping, f, indent=2)
    
    print("\nData preparation completed!")


if __name__ == "__main__":
    random.seed(42)
    np.random.seed(42)
    main()