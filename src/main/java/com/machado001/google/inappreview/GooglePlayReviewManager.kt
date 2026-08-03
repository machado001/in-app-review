package com.machado001.google.inappreview

import android.app.Activity
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GooglePlayReviewManager(
    private val reviewManager: ReviewManager,
    private val promptPolicy: ReviewPromptPolicy
) {

    /**
     * The only entry point into the review flow. Nothing is requested from Play — not even
     * [ReviewManager.requestReviewFlow] — unless [promptPolicy] says the user is due.
     *
     * The ask is recorded before the flow is launched: a failed or throttled flow still consumed
     * the user's goodwill for this session, so it must count against the lifetime cap.
     */
    suspend fun maybeLaunchReview(activity: Activity) {
        if (!promptPolicy.shouldAskForReview()) return
        promptPolicy.onReviewAsked()
        askForReview(activity)
    }

    private suspend fun askForReview(activity: Activity) {
        try {
            reviewManager.launchReviewFlow(activity, getReviewInfo())
                .addOnCompleteListener {
                    logcat(LogPriority.INFO) {
                        "launchReviewFlow: completed. is successful? ${it.isSuccessful}"
                    }
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
        }
    }

    private suspend fun getReviewInfo() =
        suspendCancellableCoroutine<ReviewInfo> { continuation ->
            reviewManager.requestReviewFlow()
                .addOnCompleteListener { request ->
                    return@addOnCompleteListener if (request.isSuccessful) {
                        continuation.resume(request.result)
                    } else {
                        val reviewErrorCode = request.exception as? ReviewException
                        if (reviewErrorCode != null) {
                            continuation.resumeWithException(reviewErrorCode)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Review request failed.", request.exception)
                            )
                        }
                    }
                }
        }
}
