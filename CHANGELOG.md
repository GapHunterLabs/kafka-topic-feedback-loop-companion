<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kafka Topic Feedback-Loop Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Whole-project, unbounded-depth (cycle-protected) transitive call
  graph resolving whether a `@KafkaListener` method's own call chain
  produces back to the same topic it consumes -- a real self-feeding
  loop, not just co-existence of a producer and consumer.

[Unreleased]: https://github.com/GapHunterLabs/kafka-topic-feedback-loop-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/kafka-topic-feedback-loop-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/kafka-topic-feedback-loop-companion/commits/0.1.0
