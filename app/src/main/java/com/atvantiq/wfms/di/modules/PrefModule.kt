package com.atvantiq.wfms.di.modules
import android.content.Context
import com.atvantiq.wfms.data.prefs.PrefMain
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object PrefModule {

    @Provides
    @Singleton
    fun providePrefMain(@ApplicationContext context: Context): PrefMain {
        return PrefMain(context)
    }

    @Provides
    @Singleton
    fun provideSecurePrefMain(@ApplicationContext context: Context): SecurePrefMain {
        return SecurePrefMain(context)
    }
}