package dev.gaphunter.kafkatopicfeedbackloopcompanion.model

import com.intellij.psi.PsiElement

/** A confirmed feedback loop: a `@KafkaListener` consumer of [topic] whose own call graph (same class, or one injected collaborator, unbounded depth, cycle-protected) transitively reaches a `.send(...)` back to that SAME topic. */
data class FeedbackLoopHit(val topic: String, val anchor: PsiElement)
