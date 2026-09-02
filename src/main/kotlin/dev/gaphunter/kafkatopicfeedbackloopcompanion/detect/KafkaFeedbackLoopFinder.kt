package dev.gaphunter.kafkatopicfeedbackloopcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import dev.gaphunter.kafkatopicfeedbackloopcompanion.model.FeedbackLoopHit

/**
 * Finds a `@KafkaListener(topics = "X")` method whose OWN transitive
 * call graph ([TransitiveProducedTopicsResolver]) reaches a
 * `.send(...)` back to topic `"X"` -- the real self-feeding loop
 * ("poison pill"/infinite reprocessing anti-pattern Confluent's own
 * guides document), never just "a producer and a consumer of the same
 * topic exist somewhere in the project" (that alone proves nothing --
 * the real path back matters).
 */
object KafkaFeedbackLoopFinder {

    fun findAll(file: PsiFile): List<FeedbackLoopHit> {
        val hits = mutableListOf<FeedbackLoopHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                hits += hitsForListener(method)
            }
        })
        return hits
    }

    private fun hitsForListener(method: PsiMethod): List<FeedbackLoopHit> {
        val topics = KafkaListenerSignals.listenedTopics(method) ?: return emptyList()
        val owningClass = method.containingClass ?: return emptyList()
        val produced = TransitiveProducedTopicsResolver.producedTopics(method, owningClass)
        val loopedTopics = topics.intersect(produced)
        if (loopedTopics.isEmpty()) return emptyList()

        val annotation = method.modifierList.annotations.first { it.nameReferenceElement?.referenceName == "KafkaListener" }
        val anchor = annotation.nameReferenceElement ?: annotation
        return loopedTopics.map { FeedbackLoopHit(it, anchor) }
    }
}
