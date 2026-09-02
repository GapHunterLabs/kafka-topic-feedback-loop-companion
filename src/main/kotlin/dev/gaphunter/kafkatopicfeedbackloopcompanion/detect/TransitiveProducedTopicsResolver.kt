package dev.gaphunter.kafkatopicfeedbackloopcompanion.detect

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

/**
 * Computes, for a method, the real set of Kafka topics its
 * TRANSITIVE call graph produces to -- same-class calls, plus ONE
 * level of injected-collaborator resolution (a field/parameter whose
 * declared type resolves to a SINGLE concrete class in the same
 * module), same collaborator-resolution technique
 * `deadlock-lock-order-companion` v0.3's `TransitiveLockResolver`
 * already proved (`resolveUniqueConcreteClassInModule`) -- ambiguous
 * collaborator types (an interface with more than one real
 * implementation) are never guessed, same as there.
 *
 * **Unlike v0.3, collaborator-hop depth is NOT bounded to a fixed
 * constant** -- a real cycle-protected visited-set (memoization +
 * `inProgress` tracking, identical mechanism to v0.3's own cycle
 * protection) is the ONLY thing that stops traversal, matching this
 * plugin's own stated goal of a genuinely unbounded whole-project
 * graph walk. The safety valve against a pathologically dense
 * collaboration graph is [MAX_METHODS_VISITED], a flat total cap on
 * how many distinct methods this resolver will ever analyze across
 * the WHOLE run (not a per-branch depth bound) -- once hit, any method
 * not already memoized is treated as producing no topics (never a
 * guess, and never a crash/hang), stated honestly here and in the
 * README.
 */
object TransitiveProducedTopicsResolver {

    private const val MAX_METHODS_PER_CLASS = 200
    const val MAX_METHODS_VISITED = 2000

    fun producedTopics(method: PsiMethod, owningClass: PsiClass): Set<String> {
        if (owningClass.methods.size > MAX_METHODS_PER_CLASS) return emptySet()
        val memo = mutableMapOf<PsiMethod, Set<String>>()
        return resolve(method, owningClass, memo, linkedSetOf())
    }

    private fun resolve(
        method: PsiMethod,
        owningClass: PsiClass,
        memo: MutableMap<PsiMethod, Set<String>>,
        inProgress: LinkedHashSet<PsiMethod>,
    ): Set<String> {
        memo[method]?.let { return it }
        if (method in inProgress) return emptySet() // real call cycle -- break it here
        if (memo.size >= MAX_METHODS_VISITED) return emptySet() // safety valve, never a guess

        inProgress += method
        val topics = mutableSetOf<String>()
        val body = method.body

        if (body != null) {
            topics += KafkaProducerSendFinder.topicsProducedIn(body)

            body.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(call)
                    val (targetClass, targetMethod) = resolveCallTarget(call, owningClass) ?: return
                    topics += resolve(targetMethod, targetClass, memo, inProgress)
                }
            })
        }

        inProgress -= method
        memo[method] = topics
        return topics
    }

    private fun resolveCallTarget(call: PsiMethodCallExpression, owningClass: PsiClass): Pair<PsiClass, PsiMethod>? {
        resolveSameClassMethod(call, owningClass)?.let { return owningClass to it }
        return resolveCollaboratorMethod(call, owningClass)
    }

    private fun resolveSameClassMethod(call: PsiMethodCallExpression, owningClass: PsiClass): PsiMethod? {
        val resolved = call.resolveMethod() ?: return null
        return resolved.takeIf { it.containingClass == owningClass }
    }

    /** Same resolution technique as `TransitiveLockResolver.resolveCollaboratorMethod` -- see that class's doc for the full reasoning on what "ambiguous" means here. */
    private fun resolveCollaboratorMethod(call: PsiMethodCallExpression, owningClass: PsiClass): Pair<PsiClass, PsiMethod>? {
        val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return null
        val resolvedVariable = qualifier.resolve() ?: return null
        val declaredType = when (resolvedVariable) {
            is PsiField -> if (resolvedVariable.containingClass == owningClass) resolvedVariable.type else return null
            is PsiParameter -> resolvedVariable.type
            else -> return null
        }
        val declaredClass = (declaredType as? PsiClassType)?.resolve() ?: return null
        val concreteClass = resolveUniqueConcreteClassInModule(declaredClass, owningClass) ?: return null

        val calledMethod = call.resolveMethod() ?: return null
        val concreteMethod = concreteClass.findMethodBySignature(calledMethod, true) ?: return null
        if (concreteMethod.body == null) return null
        return concreteClass to concreteMethod
    }

    private fun resolveUniqueConcreteClassInModule(declaredClass: PsiClass, owningClass: PsiClass): PsiClass? {
        if (!declaredClass.isInterface && !declaredClass.hasModifierProperty(PsiModifier.ABSTRACT)) return declaredClass

        val module = ModuleUtilCore.findModuleForPsiElement(owningClass) ?: return null
        val scope = GlobalSearchScope.moduleScope(module)
        val implementations = ClassInheritorsSearch.search(declaredClass, scope, true).findAll()
            .filter { !it.isInterface && !it.hasModifierProperty(PsiModifier.ABSTRACT) }
        return implementations.singleOrNull()
    }
}
