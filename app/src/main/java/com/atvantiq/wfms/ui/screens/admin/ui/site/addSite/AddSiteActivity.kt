package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityAddSiteBinding
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInClickEvents
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AssignTaskError
import retrofit2.HttpException
import java.util.Locale

class AddSiteActivity : BaseActivity<ActivityAddSiteBinding, AddSiteVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_site, AddSiteVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

        vm.clickEvents.observe(this) {
            when (it) {
                AddSiteClickEvents.ON_CANCEL_CLICK -> {
                    finish()
                }
            }
        }

        vm.errorHandler.observe(this) { error ->
            when (error) {
                AddSiteErrorHandler.ON_CLIENT_ERROR -> {
                    binding.clientEt.error = getString(R.string.select_client)
                    showToast(this, getString(R.string.select_client))
                }

                AddSiteErrorHandler.ON_PROJECT_ERROR -> {
                    binding.projectEt.error = getString(R.string.select_project)
                    showToast(this, getString(R.string.select_project))
                }

                AddSiteErrorHandler.ON_CIRCLE_ERROR -> {
                    binding.circleEt.error = getString(R.string.select_circle)
                    showToast(this, getString(R.string.select_circle))
                }

                AddSiteErrorHandler.ON_SITE_ID_ERROR -> {
                    binding.siteIdEt.error = getString(R.string.siteIdHint)
                    showToast(this, getString(R.string.siteIdHint))
                }
                AddSiteErrorHandler.ON_SITE_NAME_ERROR ->{
                    binding.siteNameEt.error = getString(R.string.siteNameHint)
                    showToast(this, getString(R.string.siteNameHint))
                }
                AddSiteErrorHandler.ON_SITE_ADDRESS_ERROR -> {
                    binding.siteAddressEt.error = getString(R.string.siteAddressHint)
                    showToast(this, getString(R.string.siteAddressHint))
                }

            }
        }

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
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
                            tokenExpiresAlert()
                        }
                    }else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
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
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
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
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
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
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
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

    private fun onClientSelected(selectedClient: Client) {
        viewModel.selectedClient = selectedClient
        binding.clientEt.setText(selectedClient.companyName)
        binding.projectEt.setText("")
        viewModel.selectedProjectId = null
        getProjectListByClientId(selectedClient.id)
    }

    private fun onProjectSelected(selectedProject: ProjectData) {
        viewModel.selectedProjectId = selectedProject.id
        binding.projectEt.setText(selectedProject.name)
        binding.circleEt.setText("")
        viewModel.selectedCircleId = null
        getCircleListByProject(selectedProject.id)
    }

    private fun showClientSelectionDialog(clients: List<Client>) {
        showSelectionDialog(
            items = clients,
            title = getString(R.string.select_client),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, client ->
                view.findViewById<TextView>(R.id.text1).text = client.companyName
            },
            onItemSelected = {
                binding.clientEt.error = null
                onClientSelected(it)
            },
            filterCondition = { client, query ->
                client.companyName.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_clients_available),
            retryAction = { getClientList() },
            tag = "ClientSelectionDialog"
        )
    }

    private fun showProjectSelectionDialog(projects: List<ProjectData>) {
        showSelectionDialog(
            items = projects,
            title = getString(R.string.select_project),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, project ->
                view.findViewById<TextView>(R.id.text1).text = project.name
            },
            onItemSelected = {
                binding.projectEt.error = null
                onProjectSelected(it)
            },
            filterCondition = { project, query ->
                project.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_projects_available),
            retryAction = { getProjectListByClientId(viewModel.selectedClient?.id ?: 0L) },
            tag = "ProjectSelectionDialog"
        )
    }

    private fun showCircleSelectionDialog(circles: List<CircleData>) {
        showSelectionDialog(
            items = circles,
            title = getString(R.string.select_circle),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, circle ->
                view.findViewById<TextView>(R.id.text1).text = circle.name
            },
            onItemSelected = {
                binding.circleEt.error = null
                viewModel.selectedCircleId = it.id
                binding.circleEt.setText(it.name)
            },
            filterCondition = { circle, query ->
                circle.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_circles_available),
            retryAction = { getCircleListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "CircleSelectionDialog"
        )
    }

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