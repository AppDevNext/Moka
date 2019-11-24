package com.moka.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.*

@Suppress("UnstableApiUsage")
class MokaUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> {
        return Arrays.asList("getSharedPreferences") //"edit",
    }

    override fun visitMethod(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = node.methodName
        val evaluator = context.evaluator

        if (evaluator.isMemberInClass(method, "android.content.SharedPreferences")) {
            System.err.println("lint found $method")
            val fix = quickFixIssueLog(node)
            context.report(ISSUE_SHARED_PREFS, node, context.getLocation(node), "Using 'SharedPreferences' instead of 'DBPrefs'", fix)
            return
        } else
            System.err.println("lint not found $method")
        //        if (evaluator.isMemberInClass(method, "android.content.SharedPreferences")) {
        //            LintFix fix = quickFixIssueLog(call);
        //            context.report(ISSUE_SHARED_PREFS, call, context.getLocation(call), "Using 'SharedPreferences' instead of 'DBPrefs'", fix);
        //            return;
        //        }
    }

//    override fun visitMethod(context: JavaContext, node: UCallExpression, method: PsiMethod) {
//        val methodName = node.methodName
//        val evaluator = context.evaluator
//
//        if (evaluator.isMemberInClass(method, "androidx.test.espresso.Espresso.onView")) {
//            System.err.println("Espresso.onView found $method")
//            val fix = quickFixIssueLog(node)
//            context.report(ISSUE_ESPRESSO_ONVIEW, node, context.getLocation(node), "Using 'Espresso.onView()' instead of 'com.moka.EspressoMoka.onView()'", fix)
//            return
//        } else
//            println("Espresso.onView not found $method")
//    }

    private fun quickFixIssueLog(logCall: UCallExpression): LintFix {
        val fixSource2 = "DBPrefs()"

        val logCallSource = logCall.asSourceString()
        val fixGrouper = fix().group()
        fixGrouper.add(fix().replace().text(logCallSource).shortenNames().reformat(true).with(fixSource2).build())
        return fixGrouper.build()
    }

    companion object {

        internal val issues: Array<Issue>
            get() = arrayOf(ISSUE_SHARED_PREFS)

        @Suppress("SpellCheckingInspection")
        val ISSUE_ESPRESSO_ONVIEW = Issue.create("Espresso.onView", "Espresso.onView() is used, use com.moka.EspressoMoka.onView() instead",
                "Since Espresso.onView() are used in the project, it can happen that some flakiness can happen",
                Category.USABILITY, 5, Severity.ERROR,
                Implementation(MokaUsageDetector::class.java, Scope.JAVA_FILE_SCOPE))

        val ISSUE_SHARED_PREFS = Issue.create("SharedPrefsUsage", "SharedPreferences are used, use DBPrefs instead",
                "Since SharedPreferences are used in the project, it is likely that calls to Preferences should instead be going to DBPrefs.",
                Category.USABILITY, 5, Severity.WARNING,
                Implementation(MokaUsageDetector::class.java, Scope.JAVA_FILE_SCOPE))
    }
}
