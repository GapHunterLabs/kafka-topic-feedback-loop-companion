# Kafka Topic Feedback-Loop Companion

Flags a `@KafkaListener` method whose own transitive call graph
reaches a `.send(...)` back to the SAME topic it consumes.

## Why it exists

A real self-feeding/"poison pill" loop -- a message that triggers its
own reprocessing indefinitely can saturate an entire consumer group,
documented as a real anti-pattern in Confluent's own guides on
avoiding cyclic topic dependencies. None of this catalog's other three
Kafka plugins (`kafka-producer-reuse-companion`, `kafka-topic-schema-
companion`, `kafka-premature-offset-commit-companion`) cover this
angle -- client reuse, schema, and commit timing respectively, never
the topic-to-topic call graph. No dedicated Marketplace plugin found.

## Why built this way

- **A real transitive call graph, not "a producer and consumer of the
  same topic both exist somewhere"** -- that alone proves nothing; the
  actual path from the consumer back to a producer of the same topic is
  what confirms a genuine loop.
- **Unbounded depth, cycle-protected only** -- reuses the exact
  collaborator-resolution technique `deadlock-lock-order-companion`
  v0.3's `TransitiveLockResolver` already proved (a field/parameter
  whose declared type resolves to a SINGLE concrete class in the same
  module; an ambiguous interface with more than one real implementation
  is never guessed), but WITHOUT v0.3's fixed collaborator-depth bound
  -- only a real visited-set (memoization + in-progress cycle
  detection) stops traversal, plus a flat total-methods-visited safety
  valve ([TransitiveProducedTopicsResolver.MAX_METHODS_VISITED]) as the
  honest resource-usage cap.

## v0.1 scope — stated honestly, not exhaustively

- Only Spring Kafka's `@KafkaListener` (consumer side) and
  `KafkaTemplate.send(...)`/`KafkaProducer.send(new
  ProducerRecord<>(...))` (producer side) -- a manual
  `KafkaConsumer.subscribe(...)` poll-loop consumer is out of scope
  (the angle `kafka-premature-offset-commit-companion` already covers,
  for a different problem).
- Only string-literal topic names -- a topic name built from a
  variable/constant reference is never followed.
- Collaborator resolution only within the same module; an interface
  collaborator type with more than one real implementation in that
  module is genuine ambiguity, never resolved by guessing.

## Usage

Open a Java class with a `@KafkaListener(topics = "orders")` method
whose call chain eventually sends back to `"orders"` -- the
`@KafkaListener` annotation shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
