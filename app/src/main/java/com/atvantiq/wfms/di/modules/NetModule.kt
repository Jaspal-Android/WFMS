package com.atvantiq.wfms.di.modules

import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.network.NetworkEndPoints
import com.atvantiq.wfms.BuildConfig
import com.facebook.stetho.okhttp3.StethoInterceptor
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
    fun provideOkhttpClient(): OkHttpClient {
        val client = OkHttpClient.Builder()
        client.readTimeout(TIME_OUT, TimeUnit.MINUTES)
        client.connectTimeout(TIME_OUT, TimeUnit.MINUTES)
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        client.addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                //  .addHeader("language", MApplication.language)
                
                .build()
            chain.proceed(request)
        }
        val protocols: MutableList<Protocol> = ArrayList()
        protocols.add(Protocol.HTTP_1_1)
        //protocols.add(Protocol.HTTP_2)
        client.protocols(protocols)
        client.addInterceptor(logging)
        if (BuildConfig.DEBUG) {
            client.addNetworkInterceptor(StethoInterceptor())
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
        const val TIME_OUT: Long = 2
    }
}
