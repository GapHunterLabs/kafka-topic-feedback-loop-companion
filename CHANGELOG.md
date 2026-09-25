<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kafka Topic Feedback-Loop Companion Changelog

## [Unreleased]

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
