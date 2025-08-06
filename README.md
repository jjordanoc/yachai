# YachAI: Tu Compañero de Matemáticas

> **Yacha** (verb) - Quechua origin, meaning "to know; to understand, the act of acquiring or possessing knowledge"

YachAI is an offline mobile AI math tutor designed specifically for Peruvian students aged 10-12 (5th-7th grade) in rural Peru. Leveraging Google's Gemma 3n E2B Vision model, it delivers visual instruction in Spanish without internet connectivity, transforming any Android device into a personal math problem-solving teacher using think-aloud methodology and procedural animation generation.

## 🌟 Problem Statement

The COVID-19 pandemic only broadened existing inequalities in education in Peru, a country of 34 million people. Out of 8 million students of school age, around 3 million live in extreme poverty in areas with limited or no access to the internet. Moreover, 8 million adults haven't finished school. The question naturally arises: **Who is going to help these children at home?**

## 🚀 Features

- **Offline-First Design**: Complete functionality without internet connectivity
- **Multimodal Input**: Support for both text and handwritten math problems via camera
- **Visual Learning**: Interactive whiteboard with step-by-step animations
- **Think-Aloud Methodology**: AI tutor verbalizes reasoning process
- **Spanish Language Support**: Native Peruvian Spanish interaction
- **Adaptive Learning**: Personalized problem generation based on student progress
- **Rural-Focused UI**: Designed specifically for young students in rural areas

## 🛠️ Setup & Installation

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Kotlin 1.8+
- At least 4GB of free storage space for the AI model

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/yachai.git
   cd yachai
   ```

2. **Configure Hugging Face Token**

   Open `app/src/main/java/com/jjordanoc/yachai/ui/screens/OnboardingViewModel.kt` and replace `"YOUR_HF_TOKEN"` with your actual Hugging Face token:

   ```kotlin
   .putString(ModelDownloadWorker.KEY_AUTH_TOKEN, "your_actual_hf_token_here")
   ```

   To get a Hugging Face token:
   - Go to [Hugging Face](https://huggingface.co/settings/tokens)
   - Create a new token with read permissions
   - Copy the token and replace `"YOUR_HF_TOKEN"` in the code

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository and select it

4. **Sync and Build**
   - Wait for Gradle sync to complete
   - Build the project (Build → Make Project)

### Running the App

1. **Connect a device (not emulator)**
   - Ensure your device has at least 4GB of free storage

2. **Run the app**
   - Click the "Run" button in Android Studio
   - Select your target device
   - The app will install and launch

3. **First-time setup**
   - The app will guide you through onboarding
   - Download the 3GB Gemma 3n model (this may take 10-15 minutes)
   - Once downloaded, the model will be cached locally

## 🏗️ Architecture

YachAI is built on three main layers:

### 1. LLM Processing Layer
The core backend of the application that provides:
- Common interface extendible for future models
- Background model downloading with resume capability
- Model initialization (loading into memory)
- Inference logic with MediaPipe integration
- Specialized LLM prompts for educational context

### 2. Animation Layer
Intermediate layer between LLM and UI components that provides:
- Animation tools the LLM can call as text descriptions
- Leverages Gemma 3n's instruction tuning and function calling capabilities
- Implementations for each animation tool (rectangles, expressions, grids)
- Chalkboard composable using wrap-around grid layout

### 3. UI Layer
Clean and friendly UI designed for children and math problem solving:
- Clear loading screens for model initialization and inference
- ChatGPT-like input screen with reusable components
- Classroom-themed screen with whiteboard and animated alpaca character
- Chat modal for asking questions during explanations

## 🤖 How Gemma 3n Powers YachAI

- **Multilingual Support**: Native Spanish interaction with future Quechua support
- **Multimodal Understanding**: Text and image inputs for comprehensive problem solving
- **Performance**: Outstanding on-device performance (2-3 minutes for complete problem solving)
- **Offline Ready**: Complete offline functionality with offline text-to-speech support


## 🎯 Usage

1. **Launch the app** and complete the initial setup
2. **Input a math problem** by:
   - Typing the problem in the text input
   - Taking a photo of a handwritten problem
   - Combining both text and image inputs
3. **Watch the AI tutor** solve the problem step-by-step with visual animations
4. **Ask follow-up questions** using the chat modal
5. **Practice with personalized problems** generated based on your progress

## 🔧 Technical Challenges & Solutions

### 2D Drawing
The hardest part was balancing LLM layer and animation layer responsibilities. We wanted the flexibility of generated content with the precision of hand-drawn animations. Our solution was to leverage Gemma 3n's function calling capabilities, producing consistent and precise animations.

### Educational UI for Rural Areas
Creating a UI suitable for educating young rural students required minimizing distractions and leveraging familiar mental models. We overcame this by studying math textbooks for 10-12 year olds and iterating the UI based on educational research.

## 🚀 Future Work

- [ ] Implement new animation commands for wider problem types/ages
- [ ] Improve LLM inference pipeline efficiency with lightweight classifiers
- [ ] Custom prompts and reasoning per math problem type
- [ ] Fine-tuning on Quechua + math reasoning
- [ ] Support for additional subjects beyond mathematics

## 📊 Performance

- **Model Size**: ~3GB (downloaded once, cached locally)
- **Inference Time**: 2-3 minutes for complete problem solving
- **Memory Usage**: Optimized for devices with 4GB+ RAM
- **Storage**: 4GB+ free space required

---

**YachAI** - Empowering rural education through AI, one problem at a time. 🦙📚
