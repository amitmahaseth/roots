package com.rootsrecipes.di.module

import android.content.Context
import android.util.Log
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.database.entity.viewmodel.UserViewmodel
import com.rootsrecipes.di.data.ApiHelper
import com.rootsrecipes.di.data.ApiHelperImpl
import com.rootsrecipes.di.data.ApiServices
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.view.createAccount.viewModel.SignUpVM
import com.rootsrecipes.view.forgot.viewmodel.ForgotVM
import com.rootsrecipes.view.loginAccount.viewModel.SignInVM
import com.rootsrecipes.view.messages.viewModel.ChatViewModel
import com.rootsrecipes.view.messages.viewModel.MessageViewModel
import com.rootsrecipes.view.myRecipes.viewModel.MyRecipesVM
import com.rootsrecipes.view.notification.viewModel.NotificationsListVM
import com.rootsrecipes.view.onBoarding.OnBoardingVM
import com.rootsrecipes.view.recipeRecording.viewmodel.RecipeCreateViewModel
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import com.rootsrecipes.viewmodel.CommentVM
import com.rootsrecipes.viewmodel.HomeVM
import com.rootsrecipes.viewmodel.MainViewModel
import com.rootsrecipes.viewmodel.ProfileVM
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val appModule = module {
    //sharedPref instance
    single { sharedPrefHelper(androidContext()) }
    single { awsSharedPrefHelper(androidContext()) }
    //Network instance
    single { provideNetworkHelper(androidContext()) }

    single { provideOkHttpClient(get()) }
    single(named(BuildConfig.BASE_URL)) { provideRetrofit(get(), BuildConfig.BASE_URL) }
    single(named(BuildConfig.BASE_URL)) { provideApiService(get(named(BuildConfig.BASE_URL))) }
    single<ApiHelper>(named(BuildConfig.BASE_URL)) { ApiHelperImpl(get(named(BuildConfig.BASE_URL))) }

    //Repository instance
    single { MainRepository(get()) }


    //ViewModel instance
    single { OnBoardingVM() }
    single { SignUpVM(get(), get()) }
    single { SignInVM(get(), get()) }
    single { ForgotVM(get(), get()) }
    single { NotificationsListVM(get(), get()) }
    single { SettingVM(get(), get() , get()) }
    single { MyRecipesVM(get(), get()) }
    single { ProfileVM(get(), get()) }
    single { HomeVM(get(), get()) }
    single { RecipeCreateViewModel(get(), get()) }
    single { CommentVM(get(), get()) }
    single { ChatViewModel(get(),get(),get()) }
    single { MainViewModel(get()) }
    single { MessageViewModel() }
    single { UserViewmodel(get()) }
    // Add a definition for ApiHelper
    single<ApiHelper> { get(named(BuildConfig.BASE_URL)) }

}

private fun sharedPrefHelper(context: Context) = SharedPref(context)
private fun awsSharedPrefHelper(context: Context) = AWSSharedPref(context)

private fun provideNetworkHelper(context: Context) = NetworkHelper(context)

private fun provideOkHttpClient(sharedPref: SharedPref) = if (BuildConfig.DEBUG) {
    val loggingInterceptor = HttpLoggingInterceptor()
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?, authType: String?
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?, authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    loggingInterceptor.apply { loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY }
    OkHttpClient.Builder().addInterceptor(loggingInterceptor)
        .addInterceptor(TokenInterceptor(sharedPref))
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .callTimeout(300, TimeUnit.SECONDS) // Set your desired timeout here
        .connectTimeout(300, TimeUnit.SECONDS).readTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true).sslSocketFactory(SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory, trustAllCerts[0] as X509TrustManager).hostnameVerifier { _, _ -> true }
        .build()
} else OkHttpClient.Builder().addInterceptor(TokenInterceptor(sharedPref))
    .callTimeout(300, TimeUnit.SECONDS) // Set your desired timeout here
    .connectTimeout(300, TimeUnit.SECONDS).readTimeout(300, TimeUnit.SECONDS).build()


private fun provideRetrofit(
    okHttpClient: OkHttpClient, BASE_URL: String
): Retrofit =
    Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BASE_URL)
        .client(okHttpClient).build()

private fun provideApiService(retrofit: Retrofit): ApiServices =
    retrofit.create(ApiServices::class.java)


// TokenInterceptor Implementation
class TokenInterceptor(private val sharedPref: SharedPref) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = sharedPref.getString(Constants.TOKEN)
        if (token != "") {
            Log.d("tokenUser", token)
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
