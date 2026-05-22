# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Complete Hilt dependency injection setup across all ViewModels, Repositories, and the WorkManager.
- KSP (Kotlin Symbol Processing) annotation processing replacing KAPT for massive compile-time improvements.
- GitHub Actions CI/CD pipeline featuring:
  - `audit.yml`: Automated ktlint formatting, detekt static analysis, unit tests, and Debug APK generation on PRs.
  - `release.yml`: Automated cryptographically-signed production Release APK generation.
- Comprehensive professional documentation suite (README, ARCHITECTURE, SETUP, CONTRIBUTING, SECURITY, CHANGELOG).

### Changed
- Downgraded Android Gradle Plugin to AGP 8.7.3 to ensure stable ViewBinding and DataBinding metadata.
- Locked Firebase BoM to version 33.4.0 to guarantee perfect compatibility with Kotlin 2.0.21 and prevent `2.2.0` metadata clashes.
- Replaced non-deterministic `Locale.getDefault()` with `Locale.ROOT` in SimpleDateFormat instances to ensure consistent Date ID generation for Firestore across all system languages.
- Optimized `HabitAdapter` initialization within Fragments utilizing `by lazy` memory scoping to prevent redundant recreation and layout jumping.

### Fixed
- Resolved severe metadata version conflicts between modern Kotlin 2.0+ compilers and legacy Hilt/AndroidX processors.
- Secured sensitive credentials (`google-services.json` and keystores) completely from the source code via `.gitignore` exclusion and seamless GitHub Secrets integration.
- Purged dead code variables and eradicated unused imports across `HabitRepository` and UI layers.

## [1.0.0] - 2026-05-22

### Added
- Initial consolidated audit and architecture stabilization establishing the foundation for future open-source scaling.
