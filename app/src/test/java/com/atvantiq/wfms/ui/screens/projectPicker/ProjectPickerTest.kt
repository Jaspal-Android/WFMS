package com.atvantiq.wfms.ui.screens.projectPicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerUiState.Phase
import io.mockk.every
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ProjectPickerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val goa = ProjectOption(493287548012, "Goa darshan", "Chandigarh")
    private val jio = ProjectOption(508422622835, "jio_4g", null)

    private fun loaded(vararg projects: ProjectOption) = ProjectPicker().apply {
        startLoading(ProjectPickerPurpose.StartDay)
        onResponse(success = true, code = 200, message = "", projects = projects.toList())
    }

    private val ProjectPicker.current get() = state.value!!

    @Test
    fun `one project is chosen for the user`() {
        val picker = loaded(goa)

        assertEquals(Phase.CHOOSE, picker.current.phase)
        assertEquals(goa.id, picker.selectedProjectId)
        assertTrue(picker.current.canConfirm)
    }

    @Test
    fun `with several projects nothing is chosen until the user picks`() {
        val picker = loaded(goa, jio)
        assertNull(picker.selectedProjectId)
        assertFalse(picker.current.canConfirm)

        picker.select(jio.id)

        assertEquals(jio.id, picker.selectedProjectId)
        assertTrue(picker.current.canConfirm)
    }

    @Test
    fun `an empty list is the empty state, not an error`() {
        val picker = loaded()

        assertEquals(Phase.EMPTY, picker.current.phase)
        assertFalse(picker.current.canConfirm)
    }

    @Test
    fun `success false is a failure with the server's message, even with code 200`() {
        val picker = ProjectPicker().apply {
            startLoading(ProjectPickerPurpose.StartDay)
            onResponse(success = false, code = 200, message = "Not allowed.", projects = listOf(goa))
        }

        assertEquals(Phase.FAILED, picker.current.phase)
        assertEquals("Not allowed.", picker.current.failureMessage)
        assertTrue(picker.current.projects.isEmpty())
    }

    @Test
    fun `a network error is a failure with the generic text`() {
        val picker = ProjectPicker().apply {
            startLoading(ProjectPickerPurpose.StartDay)
            onError(IOException("offline"))
        }

        assertEquals(Phase.FAILED, picker.current.phase)
        assertNull(picker.current.failureMessage)
    }

    @Test
    fun `a lost session closes the picker`() {
        val unauthorized = HttpException(Response.error<Any>(401, "".toResponseBody(null)))
        val picker = ProjectPicker().apply {
            startLoading(ProjectPickerPurpose.StartDay)
            onError(unauthorized)
        }
        assertFalse(picker.isOpen)

        val rejected = ProjectPicker().apply {
            startLoading(ProjectPickerPurpose.StartDay)
            onResponse(success = false, code = 401, message = "", projects = null)
        }
        assertFalse(rejected.isOpen)
    }

    @Test
    fun `try again reloads and shows the list`() {
        val picker = ProjectPicker().apply {
            startLoading(ProjectPickerPurpose.StartDay)
            onError(IOException("offline"))
        }

        picker.startLoading(picker.current.purpose)
        assertEquals(Phase.LOADING, picker.current.phase)
        picker.onResponse(success = true, code = 200, message = "", projects = listOf(goa))

        assertEquals(Phase.CHOOSE, picker.current.phase)
    }

    @Test
    fun `an answer that arrives after the picker closed doesn't reopen it`() {
        val picker = ProjectPicker().apply { startLoading(ProjectPickerPurpose.StartDay) }
        picker.close()

        picker.onResponse(success = true, code = 200, message = "", projects = listOf(goa))

        assertFalse(picker.isOpen)
    }

    @Test
    fun `search filters by name, ignoring case`() {
        val picker = loaded(goa, jio)

        picker.search("  GOA ")

        assertEquals(listOf(goa), picker.current.visibleProjects)
    }

    @Test
    fun `the choice can't change while the request is being sent`() {
        val picker = loaded(goa, jio)
        picker.select(goa.id)
        picker.setSubmitting(true)

        picker.select(jio.id)

        assertEquals(goa.id, picker.selectedProjectId)
        assertFalse(picker.current.canConfirm)
    }

    @Test
    fun `an unknown project can't be chosen`() {
        val picker = loaded(goa, jio)

        picker.select(42L)

        assertNull(picker.selectedProjectId)
    }

    @Test
    fun `rows without an id are left out and the circle tag comes from the first circle`() {
        val project = mockk<com.atvantiq.wfms.models.allProjects.Project> {
            every { id } returns 7L
            every { name } returns "Goa darshan"
            every { circle } returns listOf(com.atvantiq.wfms.models.allProjects.Circle("CHD", 1L, "Chandigarh"))
        }
        assertEquals(ProjectOption(7L, "Goa darshan", "Chandigarh"), project.toProjectOption())

        val noId = com.atvantiq.wfms.models.project.ManagedProject(id = null, name = "x", circle = null)
        assertNull(noId.toProjectOption())
    }
}
