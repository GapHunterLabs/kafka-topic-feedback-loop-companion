package dev.gaphunter.kafkatopicfeedbackloopcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KafkaFeedbackLoopInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(KafkaFeedbackLoopInspection::class.java)
    }

    fun `test a listener that sends directly back to its own topic is flagged`() {
        myFixture.configureByText(
            "OrderListener.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;
            import org.springframework.kafka.core.KafkaTemplate;

            class OrderListener {
                private KafkaTemplate<String, String> kafkaTemplate;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    kafkaTemplate.send("orders", message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("self-feeding") == true })
    }

    fun `test a listener that sends via a same-class helper method is flagged`() {
        myFixture.configureByText(
            "OrderListener2.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;
            import org.springframework.kafka.core.KafkaTemplate;

            class OrderListener2 {
                private KafkaTemplate<String, String> kafkaTemplate;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    republish(message);
                }

                private void republish(String message) {
                    kafkaTemplate.send("orders", message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("self-feeding") == true })
    }

    fun `test a listener that sends via an injected collaborator is flagged`() {
        myFixture.configureByText(
            "OrderListener3.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;
            import org.springframework.kafka.core.KafkaTemplate;

            class OrderPublisher {
                private KafkaTemplate<String, String> kafkaTemplate;
                void publish(String message) {
                    kafkaTemplate.send("orders", message);
                }
            }

            class OrderListener3 {
                private OrderPublisher publisher;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    publisher.publish(message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("self-feeding") == true })
    }

    fun `test a listener that produces to a DIFFERENT topic is not flagged`() {
        myFixture.configureByText(
            "OrderListener4.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;
            import org.springframework.kafka.core.KafkaTemplate;

            class OrderListener4 {
                private KafkaTemplate<String, String> kafkaTemplate;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    kafkaTemplate.send("orders-processed", message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("self-feeding") == true })
    }

    fun `test a listener with no send call at all is not flagged`() {
        myFixture.configureByText(
            "OrderListener5.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;

            class OrderListener5 {
                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    System.out.println(message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("self-feeding") == true })
    }

    fun `test a plain method with the same shape but no KafkaListener annotation is not flagged`() {
        myFixture.configureByText(
            "PlainHandler.java",
            """
            import org.springframework.kafka.core.KafkaTemplate;

            class PlainHandler {
                private KafkaTemplate<String, String> kafkaTemplate;

                void onMessage(String message) {
                    kafkaTemplate.send("orders", message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("self-feeding") == true })
    }

    fun `test ProducerRecord constructor form is also recognized`() {
        myFixture.configureByText(
            "OrderListener6.java",
            """
            import org.apache.kafka.clients.producer.KafkaProducer;
            import org.apache.kafka.clients.producer.ProducerRecord;
            import org.springframework.kafka.annotation.KafkaListener;

            class OrderListener6 {
                private KafkaProducer<String, String> producer;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    producer.send(new ProducerRecord<>("orders", message));
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("self-feeding") == true })
    }

    fun `test a call cycle between two same-class methods does not hang and is not falsely flagged`() {
        myFixture.configureByText(
            "OrderListener7.java",
            """
            import org.springframework.kafka.annotation.KafkaListener;
            import org.springframework.kafka.core.KafkaTemplate;

            class OrderListener7 {
                private KafkaTemplate<String, String> kafkaTemplate;

                @KafkaListener(topics = "orders")
                void onMessage(String message) {
                    helperA(message);
                }

                private void helperA(String message) {
                    helperB(message);
                }

                private void helperB(String message) {
                    kafkaTemplate.send("unrelated-topic", message);
                    helperA(message);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("self-feeding") == true })
    }
}
