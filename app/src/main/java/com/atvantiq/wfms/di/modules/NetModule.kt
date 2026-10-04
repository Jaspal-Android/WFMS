package com.atvantiq.wfms.di.modules

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.network.AuthInterceptor
import com.atvantiq.wfms.network.LogRedactor
import com.atvantiq.wfms.network.NetworkEndPoints
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.debug.DebugTools
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class NetModule() {

    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        gsonBuilder. setStrictness(Strictness.LENIENT)
        return gsonBuilder.create()
    }
    
    @Provides
    @Singleton
    fun provideOkhttpClient(prefMain: SecurePrefMain): OkHttpClient {
        val client = OkHttpClient.Builder()
        client.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        client.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        client.writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val logging = HttpLoggingInterceptor { message ->
            HttpLoggingInterceptor.Logger.DEFAULT.log(LogRedactor.redact(message))
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader(AuthInterceptor.AUTHORIZATION)
        }
        // Registered before logging so the logged request shows the header (value is redacted).
        client.addInterceptor(AuthInterceptor { prefMain.get(PrefKeys.LOGIN_TOKEN, "") })
        val protocols: MutableList<Protocol> = ArrayList()
        protocols.add(Protocol.HTTP_1_1)
        //protocols.add(Protocol.HTTP_2)
        client.protocols(protocols)
        client.addInterceptor(logging)
        if (BuildConfig.DEBUG) {
            DebugTools.networkInterceptor()?.let(client::addNetworkInterceptor)
        }
        return client.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient,baseUrl:String): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(baseUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    companion object {
        // Connecting fails fast so a dead connection does not leave a spinner up for minutes.
        // Read/write stay generous because claims and work photos are uploaded as multipart bodies.
        const val CONNECT_TIMEOUT_SECONDS: Long = 15
        const val READ_TIMEOUT_SECONDS: Long = 60
        const val WRITE_TIMEOUT_SECONDS: Long = 60
    }
}
