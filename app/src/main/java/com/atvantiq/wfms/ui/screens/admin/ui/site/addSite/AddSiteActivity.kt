package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.activity.enableEdgeToEdge
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityAddSiteBinding
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class AddSiteActivity : BaseActivity<ActivityAddSiteBinding, AddSiteVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_site, AddSiteVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setUpToolbar()
        getClientList()
        initListeners()
    }

    private fun setUpToolbar() {
        binding.addSiteToolbar.toolbarTitle.text = getString(R.string.add_site)
        binding.addSiteToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: AddSiteVM) {
        binding.vm = vm

        vm.clientListResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    when (response.response?.code) {
                        ValConstants.SUCCESS_CODE -> {
                            val clients = response.response?.data?.clients ?: emptyList()
                            viewModel.clients = clients
                        }

                        ValConstants.UNAUTHORIZED_CODE -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    handleApiFailure(response.throwable)
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

        vm.projectListByClientResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isProjectLoading.set(false)
                    when (response.response?.code) {
                        ValConstants.SUCCESS_CODE -> {
                            // Handle success
                            val projects = response.response?.data ?: emptyList()
                            viewModel.projects = projects
                        }

                        ValConstants.UNAUTHORIZED_CODE -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isProjectLoading.set(false)
                    handleApiFailure(response.throwable)
                }

                Status.LOADING -> {
                    vm.isProjectLoading.set(true)
                }
            }
        }

        vm.circleListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isCircleLoading.set(false)
                    when (response.response?.code) {
                        ValConstants.SUCCESS_CODE -> {
                            val circles = response.response?.data ?: emptyList()
                            viewModel.circles = circles
                        }

                        ValConstants.UNAUTHORIZED_CODE -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isCircleLoading.set(false)
                    handleApiFailure(response.throwable)
                }

                Status.LOADING -> {
                    vm.isCircleLoading.set(true)
                }
            }
        }


        vm.createSiteResponse.observe(this)
        { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    when (response.response?.code) {
                        ValConstants.SUCCESS_CREATION_CODE -> {
                            showToast(
                                this,
                                response.response?.message ?: getString(R.string.site_created_successfully)
                            )
                            setResult(Activity.RESULT_OK)
                            finish()
                        }

                        ValConstants.UNAUTHORIZED_CODE -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    handleApiFailure(response.throwable)
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }
    }

    private fun initListeners() {
        binding.clientEt.setOnClickListener {
            showClientSelectionDialog(viewModel.clients)
        }

        binding.projectEt.setOnClickListener {
            showProjectSelectionDialog(viewModel.projects)
        }

        binding.circleEt.setOnClickListener {
            showCircleSelectionDialog(viewModel.circles)
        }
    }

    private fun showClientSelectionDialog(clients: List<Client>) {
        showSelectionDialog(
            items = clients,
            title = getString(R.string.select_client),
            layoutResId = R.layout.item_picker_option,
            bind = { view, client ->
                bindOption(view, client.companyName, client.displayName, client.id == viewModel.selectedClient?.id)
            },
            onItemSelected = { viewModel.selectClient(it) },
            filterCondition = { client, query -> matches(query, client.companyName, client.displayName) },
            emptyMessage = getString(R.string.no_clients_available),
            retryAction = { getClientList() },
            tag = "ClientSelectionDialog"
        )
    }

    private fun showProjectSelectionDialog(projects: List<ProjectData>) {
        showSelectionDialog(
            items = projects,
            title = getString(R.string.select_project),
            layoutResId = R.layout.item_picker_option,
            bind = { view, project -> bindOption(view, project.name, null, project.id == viewModel.selectedProjectId) },
            onItemSelected = { viewModel.selectProject(it) },
            filterCondition = { project, query -> matches(query, project.name) },
            emptyMessage = getString(R.string.no_projects_available),
            retryAction = { getProjectListByClientId(viewModel.selectedClient?.id ?: 0L) },
            tag = "ProjectSelectionDialog"
        )
    }

    private fun showCircleSelectionDialog(circles: List<CircleData>) {
        showSelectionDialog(
            items = circles,
            title = getString(R.string.select_circle),
            layoutResId = R.layout.item_picker_option,
            bind = { view, circle -> bindOption(view, circle.name, circle.code, circle.id == viewModel.selectedCircleId) },
            onItemSelected = { viewModel.selectCircle(it) },
            filterCondition = { circle, query -> matches(query, circle.name, circle.code) },
            emptyMessage = getString(R.string.no_circles_available),
            retryAction = { getCircleListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "CircleSelectionDialog"
        )
    }

    /** A picker row: the name in bold, an optional grey second line, and a radio for the current choice. */
    private fun bindOption(view: View, title: String?, subtitle: String?, selected: Boolean) {
        view.findViewById<TextView>(R.id.optionTitle).text = title
        view.findViewById<TextView>(R.id.optionSubtitle).apply {
            text = subtitle
            isVisible = !subtitle.isNullOrBlank()
        }
        view.findViewById<ImageView>(R.id.optionRadio)
            .setImageResource(if (selected) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked)
        view.isSelected = selected
    }

    /** The search filters on the device, on any of [fields], ignoring case. */
    private fun matches(query: String, vararg fields: String?): Boolean =
        fields.any { it.orEmpty().contains(query.trim(), ignoreCase = true) }

    private fun getClientList() {
        viewModel.getClientList()
    }
    /*
    * Get project list by client id
    * */
    private fun getProjectListByClientId(clientId: Long) {
        viewModel.getProjectListByClientId(clientId)

    }

    /*
    * Get Circle list by project id
    * */
    private fun getCircleListByProject(projectId: Long) {
        viewModel.getCircleListByProject(projectId)
    }

}