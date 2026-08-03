package com.machado001.google.inappreview

/**
 * Decides whether [GooglePlayReviewManager] may request a review right now.
 *
 * Play applies its own server-side quota, but that quota is invisible to the app and does not stop
 * the app from asking on every single launch. This module therefore never asks on its own
 * authority — the host app supplies the policy and owns the usage signals it is based on.
 */
interface ReviewPromptPolicy {
    suspend fun shouldAskForReview(): Boolean

    /** Called once per review request so the policy can advance its lifetime counters. */
    suspend fun onReviewAsked()
}
