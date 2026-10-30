package com.atvantiq.wfms.ui.screens.projectPicker

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atvantiq.wfms.R
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.AttendanceApprovalActivity
import com.google.android.material.color.MaterialColors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Renders [ProjectPickerBottomSheet] in each state of its host's [ProjectPicker]. The answers are
 * fed to the picker directly, so states the dev server can't produce on demand (no projects, a
 * single project, a long list) are covered. Needs a signed-in session: the host screen loads its
 * own list.
 */
@RunWith(AndroidJUnit4::class)
class ProjectPickerSheetTest {

    private lateinit var scenario: ActivityScenario<AttendanceApprovalActivity>
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val alpha = ProjectOption(1L, "Alpha Rollout", "North")
    private val beta = ProjectOption(2L, "Beta Fibre", null)

    @Before
    fun launch() {
        scenario = ActivityScenario.launch(AttendanceApprovalActivity::class.java)
    }

    @After
    fun finish() {
        scenario.close()
    }

    @Test
    fun noManagedProjectsOffersCloseOnly() {
        open(ProjectPickerPurpose.StartDay, emptyList())

        assertEquals(context.getString(R.string.project_picker_no_projects_title), text(R.id.title))
        assertEquals(context.getString(R.string.project_picker_no_projects_message), text(R.id.message))
        assertEquals(context.getString(R.string.close), text(R.id.btnSecondary))
        assertFalse(visible(R.id.primaryContainer))
        assertFalse(visible(R.id.searchLayout))
        assertFalse(visible(R.id.listContainer))
    }

    @Test
    fun noActiveProjectsOffersCloseOnly() {
        open(ProjectPickerPurpose.MarkPresent("Kamal"), emptyList())

        assertEquals(context.getString(R.string.project_picker_no_active_projects_title), text(R.id.title))
        assertEquals(context.getString(R.string.project_picker_no_active_projects_message), text(R.id.message))
        assertFalse(visible(R.id.primaryContainer))
    }

    @Test
    fun refusedListShowsServerMessageWithTryAgain() {
        open(ProjectPickerPurpose.StartDay, null, success = false, message = "Projects are unavailable")

        assertEquals(context.getString(R.string.project_picker_load_failed_title), text(R.id.title))
        assertEquals("Projects are unavailable", text(R.id.message))
        assertEquals(context.getString(R.string.close), text(R.id.btnSecondary))
        assertEquals(context.getString(R.string.try_again), text(R.id.btnPrimary))
        assertTrue(enabled(R.id.btnPrimary))
    }

    @Test
    fun failedLoadShowsGenericMessage() {
        scenario.onActivity {
            it.projectPicker.startLoading(ProjectPickerPurpose.StartDay)
            it.projectPicker.onError(IOException("offline"))
        }
        idle()

        assertEquals(context.getString(R.string.project_picker_load_failed_message), text(R.id.message))
        assertTrue(visible(R.id.btnPrimary))
    }

    @Test
    fun lostSessionNeverOpensTheSheet() {
        open(ProjectPickerPurpose.StartDay, null, success = false, code = 401)

        assertNull(sheet())
    }

    @Test
    fun singleProjectIsChosenAndCanBeConfirmed() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha))

        assertEquals(context.getString(R.string.start_your_day), text(R.id.btnPrimary))
        assertTrue(enabled(R.id.btnPrimary))
        assertTrue(row(0).isSelected)
        assertEquals("Alpha Rollout, North", row(0).contentDescription)
    }

    @Test
    fun withSeveralProjectsNothingIsChosenUntilTapped() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha, beta))

        assertFalse(enabled(R.id.btnPrimary))
        assertFalse(row(0).isSelected)
        assertFalse(row(1).isSelected)
        assertEquals("Beta Fibre", row(1).contentDescription)

        onMain { row(1).performClick() }

        assertTrue(enabled(R.id.btnPrimary))
        assertTrue(row(1).isSelected)
        assertEquals(2L, picker().selectedProjectId)
    }

    @Test
    fun searchIgnoresCaseAndSpacesAndSaysWhenNothingMatches() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha, beta))

        type("  BETA ")
        assertEquals(1, list().adapter?.itemCount)
        assertEquals("Beta Fibre", row(0).contentDescription)

        type("zzz")
        assertTrue(visible(R.id.noMatches))
        assertFalse(visible(R.id.listContainer))

        type("")
        assertEquals(2, list().adapter?.itemCount)
        assertFalse(visible(R.id.noMatches))
    }

    @Test
    fun markPresentNamesTheEmployee() {
        open(ProjectPickerPurpose.MarkPresent("Kamal Sharma"), listOf(alpha))

        assertEquals(context.getString(R.string.project_picker_mark_present_subtitle, "Kamal Sharma"), text(R.id.message))
        assertEquals(context.getString(R.string.mark_present), text(R.id.btnPrimary))
    }

    @Test
    fun markPresentWithoutANameStillReads() {
        open(ProjectPickerPurpose.MarkPresent(" "), listOf(alpha))

        assertEquals(context.getString(R.string.project_picker_mark_present_subtitle_unnamed), text(R.id.message))
    }

    @Test
    fun whileSubmittingTheSheetCannotBeClosedOrChanged() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha, beta))
        onMain {
            picker().select(1L)
            picker().setSubmitting(true)
        }

        assertFalse(enabled(R.id.btnSecondary))
        assertEquals(View.INVISIBLE, view<View>(R.id.btnPrimary).visibility)
        assertTrue(visible(R.id.primaryProgress))
        assertFalse(sheet()!!.isCancelable)

        onMain { row(1).performClick() }
        assertEquals(1L, picker().selectedProjectId)

        onMain { view<View>(R.id.btnSecondary).performClick() }
        assertTrue(picker().isOpen)
    }

    @Test
    fun aLongListScrollsWithTheButtonsOnScreen() {
        open(ProjectPickerPurpose.StartDay, (1L..60L).map { ProjectOption(it, "Project $it", "Circle $it") })

        val maxListHeight = context.resources.getDimensionPixelSize(R.dimen.project_picker_list_max_height)
        assertTrue(view<View>(R.id.listContainer).height <= maxListHeight)
        assertTrue(list().canScrollVertically(1))
        val button = view<View>(R.id.btnPrimary)
        val location = IntArray(2).also(button::getLocationOnScreen)
        assertTrue(location[1] + button.height <= context.resources.displayMetrics.heightPixels)
    }

    @Test
    fun typedSearchTextUsesTheAppTextColor() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha, beta))
        var expected = 0
        scenario.onActivity { expected = MaterialColors.getColor(it, R.attr.wfmsColorOnSurface, "wfmsColorOnSurface") }

        type("Alpha")

        assertEquals(expected, view<EditText>(R.id.searchField).currentTextColor)
    }

    @Test
    fun closingThePickerDismissesTheSheet() {
        open(ProjectPickerPurpose.StartDay, listOf(alpha))

        onMain { view<View>(R.id.btnSecondary).performClick() }

        assertFalse(picker().isOpen)
        assertNull(sheet())
    }

    private fun open(
        purpose: ProjectPickerPurpose,
        projects: List<ProjectOption>?,
        success: Boolean = true,
        code: Int = 200,
        message: String? = null
    ) = onMain {
        picker().startLoading(purpose)
        picker().onResponse(success, code, message, projects)
    }

    private fun onMain(block: () -> Unit) {
        scenario.onActivity { block() }
        idle()
    }

    private fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    private fun picker(): ProjectPicker {
        var picker: ProjectPicker? = null
        scenario.onActivity { picker = it.projectPicker }
        return picker!!
    }

    private fun sheet(): ProjectPickerBottomSheet? {
        var sheet: ProjectPickerBottomSheet? = null
        scenario.onActivity {
            it.supportFragmentManager.executePendingTransactions()
            sheet = it.supportFragmentManager.findFragmentByTag(ProjectPickerBottomSheet.TAG) as? ProjectPickerBottomSheet
        }
        return sheet
    }

    private fun <T : View> view(id: Int): T = sheet()!!.requireView().findViewById(id)

    private fun text(id: Int): String = (view<View>(id) as TextView).text.toString()

    private fun visible(id: Int): Boolean = view<View>(id).isShown

    private fun enabled(id: Int): Boolean = view<Button>(id).isEnabled

    private fun list(): RecyclerView = view(R.id.projectList)

    private fun row(position: Int): View = list().findViewHolderForAdapterPosition(position)!!.itemView
        .findViewById(R.id.row)

    private fun type(query: String) = onMain { view<EditText>(R.id.searchField).setText(query) }
}
