# Featherweight - Weightlifting App Project Overview

## Project Purpose
A weightlifting Super App that combines features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Built for intermediate and advanced lifters with intelligent weight suggestions and transparent progressive overload logic.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Database**: Room with SQLite (destructive migration during development)
- **Architecture**: MVVM with Repository layer
- **Navigation**: Single-Activity with enum-based routing
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Design**: Athletic Elegance with glassmorphism (light theme only)

## Key Features
- Exercise Database (500 curated exercises)
- Programme System with progressive overload
- Workout tracking with smart weight input
- AI Programme Generation (OpenAI integration)
- Analytics & History with interactive charts
- Rest Timer with smart auto-start
- PR detection and celebration

## Development Guidelines
- Foundation-first approach
- Fail-fast philosophy (no mock fallbacks)
- Destructive database migrations always
- Clean code maintenance
- Transparent logic (every weight calculation explainable)