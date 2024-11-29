package com.atvantiq.wfms.di.modules

import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.data.repository.auth.AuthRepo
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
}
