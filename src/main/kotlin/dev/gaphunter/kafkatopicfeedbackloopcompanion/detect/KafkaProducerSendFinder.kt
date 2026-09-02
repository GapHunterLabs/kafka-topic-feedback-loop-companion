package dev.gaphunter.kafkatopicfeedbackloopcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression

/**
 * Finds every `.send(...)` call textually inside [scope] (never
 * descending into the body of some OTHER method a nested call resolves
 * to -- that's [TransitiveProducedTopicsResolver]'s job) and resolves
 * the topic it produces to, for the two real forms:
 * `KafkaTemplate.send(topic, ...)` (Spring convenience overload, topic
 * is the first argument directly) and
 * `KafkaProducer.send(new ProducerRecord<>(topic, ...))` (official
 * client, topic is the first argument of the `ProducerRecord`
 * constructor).
 *
 * **v0.1 scope:** only a string-literal topic argument is resolved --
 * a topic built from a variable/constant/concatenation is never
 * followed (documented honestly, same discipline as
 * [KafkaListenerSignals]).
 */
object KafkaProducerSendFinder {

    fun topicsProducedIn(scope: PsiElement): Set<String> {
        val topics = mutableSetOf<String>()
        scope.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                if (call.methodExpression.referenceName != "send") return
                val firstArgument = call.argumentList.expressions.getOrNull(0) ?: return
                topicOf(firstArgument)?.let { topics += it }
            }
        })
        return topics
    }

    private fun topicOf(firstArgument: PsiElement): String? {
        (firstArgument as? PsiLiteralExpression)?.value?.let { return it as? String }

        val newExpression = firstArgument as? PsiNewExpression ?: return null
        if (newExpression.classReference?.referenceName != "ProducerRecord") return null
        val topicArgument = newExpression.argumentList?.expressions?.getOrNull(0) as? PsiLiteralExpression ?: return null
        return topicArgument.value as? String
    }
}
