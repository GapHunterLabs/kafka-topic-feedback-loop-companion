import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

class OrderListener {
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "orders")
    void onMessage(String message) {
        kafkaTemplate.send("orders", message);
    }
}
