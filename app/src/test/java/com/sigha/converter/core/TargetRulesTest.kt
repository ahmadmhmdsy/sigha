package com.sigha.converter.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The workbench-side rule that keeps silent videos from offering audio targets. */
class TargetRulesTest {

    @Test
    fun silentVideoCannotReachAudioTargets() {
        assertFalse(supportsTargetCategory(FormatCategory.VIDEO, false, FormatCategory.AUDIO))
    }

    @Test
    fun videoWithAudioCanReachAudioTargets() {
        assertTrue(supportsTargetCategory(FormatCategory.VIDEO, true, FormatCategory.AUDIO))
    }

    @Test
    fun unknownAudioFactKeepsAudioTargets() {
        // A failed probe must be optimistic: hiding a target that could work is worse than a
        // conversion that reports its own failure.
        assertTrue(supportsTargetCategory(FormatCategory.VIDEO, null, FormatCategory.AUDIO))
    }

    @Test
    fun silentVideoStillReachesNonAudioTargets() {
        assertTrue(supportsTargetCategory(FormatCategory.VIDEO, false, FormatCategory.VIDEO))
        assertTrue(supportsTargetCategory(FormatCategory.VIDEO, false, FormatCategory.IMAGE))
    }

    @Test
    fun audioFactsOfNonVideoSourcesAreIrrelevant() {
        assertTrue(supportsTargetCategory(FormatCategory.AUDIO, false, FormatCategory.AUDIO))
        assertTrue(supportsTargetCategory(FormatCategory.IMAGE, null, FormatCategory.PDF))
        assertTrue(supportsTargetCategory(FormatCategory.PDF, false, FormatCategory.AUDIO))
    }

    @Test
    fun hidesAudioTargetsWhenAnySelectedVideoIsSilent() {
        assertTrue(
            hidesAudioTargets(
                listOf(
                    FormatCategory.VIDEO to true,
                    FormatCategory.VIDEO to false
                )
            )
        )
        assertFalse(hidesAudioTargets(listOf(FormatCategory.VIDEO to true, FormatCategory.AUDIO to null)))
        assertFalse(hidesAudioTargets(listOf(FormatCategory.VIDEO to null)))
        assertFalse(hidesAudioTargets(emptyList()))
    }
}
