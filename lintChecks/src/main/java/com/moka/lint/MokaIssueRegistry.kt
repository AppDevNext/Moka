package com.moka.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class MokaIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(*MokaUsageDetector.issues)

    override val api: Int
        get() = CURRENT_API
}
