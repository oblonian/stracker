package com.oblonian.stracker.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.oblonian.stracker.domain.WorkingHours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettingsRepositoryTest {

    private val repo = SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun `defaults are weekday 9 to 18 and weekend off`() = runBlocking {
        val s = repo.settings.first()
        assertEquals(WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)), s.workingHours[DayOfWeek.MONDAY])
        assertEquals(WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)), s.workingHours[DayOfWeek.FRIDAY])
        assertNull(s.workingHours[DayOfWeek.SATURDAY])
        assertNull(s.workingHours[DayOfWeek.SUNDAY])
        assertEquals(LocalTime.of(21, 0), s.sleepWindowStart)
        assertEquals(LocalTime.of(11, 0), s.sleepWindowEnd)
    }

    @Test
    fun `working hours survive a round trip`() = runBlocking {
        repo.setWorkingHours(DayOfWeek.SATURDAY, WorkingHours(LocalTime.of(10, 30), LocalTime.of(14, 0)))
        repo.setWorkingHours(DayOfWeek.MONDAY, null)
        val s = repo.settings.first()
        assertEquals(WorkingHours(LocalTime.of(10, 30), LocalTime.of(14, 0)), s.workingHours[DayOfWeek.SATURDAY])
        assertNull(s.workingHours[DayOfWeek.MONDAY])
    }
}
