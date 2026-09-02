package dev.gaphunter.kafkatopicfeedbackloopcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.kafkatopicfeedbackloopcompanion.detect.KafkaFeedbackLoopFinder
import dev.gaphunter.kafkatopicfeedbackloopcompanion.model.FeedbackLoopHit
import dev.gaphunter.kafkatopicfeedbackloopcompanion.review.ReviewPrompt

/**
 * Flags a `@KafkaListener` method whose own transitive call graph
 * (same class, or one injected collaborator, unbounded depth,
 * cycle-protected) reaches a `.send(...)` back to the SAME topic it
 * consumes -- a real self-feeding loop. See
 * [dev.gaphunter.kafkatopicfeedbackloopcompanion.detect.KafkaFeedbackLoopFinder].
 */
class KafkaFeedbackLoopInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = KafkaFeedbackLoopFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.topic}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: FeedbackLoopHit): String =
        "This @KafkaListener consumes topic '${hit.topic}', and its own call graph transitively produces back to the " +
            "SAME topic -- a real self-feeding/poison-pill loop that can saturate the consumer group under load"
}
