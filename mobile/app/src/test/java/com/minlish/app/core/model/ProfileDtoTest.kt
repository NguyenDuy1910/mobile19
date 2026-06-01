package com.minlish.app.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDtoTest {
    @Test
    fun completeProfileRequiresNameGoalAndLevel() {
        assertTrue(ProfileDto("1", "Min", "IELTS", "B1").isComplete)
        assertFalse(ProfileDto("1", "Min").isComplete)
        assertFalse(ProfileDto("1", "", "IELTS", "B1").isComplete)
    }
}
