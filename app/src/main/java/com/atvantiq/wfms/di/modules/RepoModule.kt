package com.atvantiq.wfms.di.modules

import com.atvantiq.wfms.data.repository.atten.AttendanceRepo
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.data.repository.auth.AuthRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.data.repository.tracking.TrackingRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.data.repository.work.WorkRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class RepoModule() {

	@Provides
	@Singleton
	fun provideIAuthRepo(authRepo: AuthRepo): IAuthRepo = authRepo

	@Provides
	@Singleton
	fun provideIAttendanceRepo(attendanceRepo: AttendanceRepo): IAttendanceRepo = attendanceRepo

	@Provides
	@Singleton
	fun provideIWorkRepo(workRepo: WorkRepo): IWorkRepo = workRepo

	@Provides
	@Singleton
	fun provideICreationRepo(creationRepo: CreationRepo): ICreationRepo = creationRepo

	@Provides
	@Singleton
	fun provideITrackingRepo(trackingRepo: TrackingRepo): ITrackingRepo = trackingRepo
}
