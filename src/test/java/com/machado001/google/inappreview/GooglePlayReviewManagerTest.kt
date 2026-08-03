package com.machado001.google.inappreview

import android.app.Activity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import java.util.concurrent.Executor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePlayReviewManagerTest {

    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().get()

    @Test
    fun `does not touch Play at all when the policy says no`() = runTest {
        val reviewManager = RecordingReviewManager()
        val policy = FakeReviewPromptPolicy(shouldAsk = false)

        GooglePlayReviewManager(reviewManager, policy).maybeLaunchReview(activity)

        assertEquals(
            "the review flow must not even be requested when the user is not due",
            0,
            reviewManager.requestCount
        )
        assertEquals(0, reviewManager.launchCount)
        assertEquals(0, policy.askedCount)
    }

    @Test
    fun `never asks across repeated launches while the policy says no`() = runTest {
        // The original bug: every launch from the second onwards hit Play unconditionally.
        val reviewManager = RecordingReviewManager()
        val policy = FakeReviewPromptPolicy(shouldAsk = false)
        val manager = GooglePlayReviewManager(reviewManager, policy)

        repeat(25) { manager.maybeLaunchReview(activity) }

        assertEquals(0, reviewManager.requestCount)
        assertEquals(25, policy.consultCount)
    }

    @Test
    fun `requests the review flow when the policy says yes`() = runTest {
        val reviewManager = RecordingReviewManager()
        val policy = FakeReviewPromptPolicy(shouldAsk = true)

        GooglePlayReviewManager(reviewManager, policy).maybeLaunchReview(activity)

        assertEquals(1, reviewManager.requestCount)
    }

    @Test
    fun `records the ask so the lifetime cap advances`() = runTest {
        val reviewManager = RecordingReviewManager()
        val policy = FakeReviewPromptPolicy(shouldAsk = true)

        GooglePlayReviewManager(reviewManager, policy).maybeLaunchReview(activity)

        assertEquals(1, policy.askedCount)
    }

    @Test
    fun `a Play failure is swallowed and still counts against the cap`() = runTest {
        // A device where Play never returns a ReviewInfo must not be retried on every launch.
        val reviewManager = RecordingReviewManager()
        val policy = FakeReviewPromptPolicy(shouldAsk = true)

        GooglePlayReviewManager(reviewManager, policy).maybeLaunchReview(activity)

        assertEquals(1, policy.askedCount)
        assertEquals("no flow can be launched without a ReviewInfo", 0, reviewManager.launchCount)
    }

    private class FakeReviewPromptPolicy(
        private val shouldAsk: Boolean
    ) : ReviewPromptPolicy {
        var consultCount = 0
            private set
        var askedCount = 0
            private set

        override suspend fun shouldAskForReview(): Boolean {
            consultCount += 1
            return shouldAsk
        }

        override suspend fun onReviewAsked() {
            askedCount += 1
        }
    }

    /**
     * [requestReviewFlow] always fails: a real [ReviewInfo] cannot be constructed outside Play
     * services, and the failure path is the one that has to stay quiet anyway.
     */
    private class RecordingReviewManager : ReviewManager {
        var requestCount = 0
            private set
        var launchCount = 0
            private set

        override fun requestReviewFlow(): Task<ReviewInfo> {
            requestCount += 1
            return FailedTask(IllegalStateException("no Play services in tests"))
        }

        override fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Task<Void> {
            launchCount += 1
            return FailedTask(IllegalStateException("not reachable in tests"))
        }
    }

    /**
     * Completes its listeners inline. `Tasks.forException` would post to the main looper, which
     * Robolectric keeps paused while the test coroutine is suspended waiting on it.
     */
    private class FailedTask<T>(private val error: Exception) : Task<T>() {
        override fun isComplete(): Boolean = true

        override fun isSuccessful(): Boolean = false

        override fun isCanceled(): Boolean = false

        override fun getResult(): T = throw error

        override fun <X : Throwable> getResult(exceptionType: Class<X>): T = throw error

        override fun getException(): Exception = error

        override fun addOnCompleteListener(listener: OnCompleteListener<T>): Task<T> = apply {
            listener.onComplete(this)
        }

        override fun addOnSuccessListener(listener: OnSuccessListener<in T>): Task<T> = this

        override fun addOnSuccessListener(
            activity: Activity,
            listener: OnSuccessListener<in T>
        ): Task<T> = this

        override fun addOnSuccessListener(
            executor: Executor,
            listener: OnSuccessListener<in T>
        ): Task<T> = this

        override fun addOnFailureListener(listener: OnFailureListener): Task<T> = apply {
            listener.onFailure(error)
        }

        override fun addOnFailureListener(
            activity: Activity,
            listener: OnFailureListener
        ): Task<T> = addOnFailureListener(listener)

        override fun addOnFailureListener(
            executor: Executor,
            listener: OnFailureListener
        ): Task<T> = addOnFailureListener(listener)
    }
}
