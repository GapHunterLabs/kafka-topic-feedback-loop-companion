package dev.gaphunter.kafkatopicfeedbackloopcompanion.detect

import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod

/**
 * Extracts the topic(s) a Spring Kafka `@KafkaListener(topics = ...)`
 * method consumes from -- a closed, known annotation attribute, same
 * "real framework signal, not general inference" discipline as this
 * catalog's `ControllerEndpointSignals`-style detectors.
 *
 * **v0.1 scope, stated honestly:** only the raw official
 * `org.apache.kafka:kafka-clients` producer form and Spring Kafka's
 * `@KafkaListener` consumer form -- a manual `KafkaConsumer.subscribe(...)`
 * poll-loop consumer (the form `kafka-premature-offset-commit-
 * companion` already covers for a DIFFERENT angle) is out of scope for
 * this plugin's v0.1, left for a future version; only string-literal
 * topic names are resolved -- a topic name built from a variable/
 * constant reference is never followed.
 */
object KafkaListenerSignals {

    fun listenedTopics(method: PsiMethod): Set<String>? {
        val annotation = method.modifierList.annotations.firstOrNull { it.nameReferenceElement?.referenceName == "KafkaListener" } ?: return null
        val topicsAttribute = annotation.findAttributeValue("topics") ?: return null
        val topics = literalStringsOf(topicsAttribute)
        return topics.ifEmpty { null }
    }

    private fun literalStringsOf(value: PsiAnnotationMemberValue): Set<String> = when (value) {
        is PsiArrayInitializerMemberValue -> value.initializers.mapNotNull { (it as? PsiLiteralExpression)?.value as? String }.toSet()
        is PsiLiteralExpression -> setOfNotNull(value.value as? String)
        else -> emptySet()
    }
}
