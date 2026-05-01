package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.data.local.dao.DailyGoalDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDailyGoalUseCaseTest {

    private val dao: DailyGoalDao = mockk(relaxed = true)
    private val useCase = RecordDailyGoalUseCase(dao)

    @Test
    fun `returns true on first achievement of the day`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns null

        val result = useCase("2024-01-01", goalSeconds = 3600, achievedSeconds = 4000)

        assertTrue("First achievement should return true", result)

        val slot = slot<DailyGoalEntity>()
        coVerify { dao.upsert(capture(slot)) }
        assertEquals("2024-01-01", slot.captured.date)
        assertEquals(3600, slot.captured.goalSeconds)
        assertEquals(4000, slot.captured.achievedSeconds)
        assertTrue(slot.captured.achieved)
    }

    @Test
    fun `returns false when goal is not yet achieved`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns null

        val result = useCase("2024-01-01", goalSeconds = 3600, achievedSeconds = 2000)

        assertFalse("Should return false when goal not met", result)

        val slot = slot<DailyGoalEntity>()
        coVerify { dao.upsert(capture(slot)) }
        assertFalse(slot.captured.achieved)
    }

    @Test
    fun `returns false when already achieved earlier`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns DailyGoalEntity(
            date = "2024-01-01",
            goalSeconds = 3600,
            achievedSeconds = 4000,
            achieved = true
        )

        val result = useCase("2024-01-01", goalSeconds = 3600, achievedSeconds = 5000)

        assertFalse("Should return false when already achieved", result)
    }

    @Test
    fun `once achieved, stays achieved even if subsequent update is below goal`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns DailyGoalEntity(
            date = "2024-01-01",
            goalSeconds = 3600,
            achievedSeconds = 4000,
            achieved = true
        )

        val result = useCase("2024-01-01", goalSeconds = 3600, achievedSeconds = 500)

        assertFalse("Already achieved so returns false", result)

        val slot = slot<DailyGoalEntity>()
        coVerify { dao.upsert(capture(slot)) }
        assertTrue("Achieved flag should persist", slot.captured.achieved)
    }

    @Test
    fun `exactly at goal boundary counts as achieved`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns null

        val result = useCase("2024-01-01", goalSeconds = 3600, achievedSeconds = 3600)

        assertTrue("Exactly at goal should count as achieved", result)
    }

    @Test
    fun `goalSeconds of zero with any positive achievedSeconds is achieved`() = runTest {
        coEvery { dao.getByDate("2024-01-01") } returns null

        val result = useCase("2024-01-01", goalSeconds = 0, achievedSeconds = 1)

        assertTrue("Should be achieved when goal is 0", result)
    }
}
