package cy.ac.ucy.cs.anyplace.smas.di

import android.app.Application
import cy.ac.ucy.cs.anyplace.smas.BuildConfig
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderSmas
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Specializing [OkHttpClient], only to make DI to pick a version with authenticator set*/
data class OkHttpClientBearer(val client: OkHttpClient)

/**
 * Dependency Injection for the Chat backend
 */
@Module
@InstallIn(SingletonComponent::class)
class ChatNetworkModule {

  @Singleton
  @Provides
  fun provideHttpClientWithBearer(): OkHttpClientBearer {
    return OkHttpClientBearer(OkHttpClient.Builder()
            .authenticator(BearerAuthenticator())
            .readTimeout(15, TimeUnit.SECONDS) // TODO: Make SETTINGS
            .connectTimeout(15, TimeUnit.SECONDS)
            .build())
  }

  @Singleton
  @Provides
  @Inject
  fun provideRetrofitHolderChat(
          app: Application, // injected from ContextModule
          okHttpClientBearer: OkHttpClientBearer,
          gsonConverterFactory: GsonConverterFactory,
  ): RetrofitHolderSmas {
    val baseUrl = RetrofitHolderSmas.getDefaultBaseUrl(app)

    return RetrofitHolderSmas(app, okHttpClientBearer.client, gsonConverterFactory).set(baseUrl)
  }
}

/**
 * Bearer Authentication for the SMAS Chat
 */
class BearerAuthenticator : Authenticator {
  override fun authenticate(route: Route?, response: Response): Request? {
    if (response.request.header("Authorization") != null) {
      return null
    }
    val token = BuildConfig.LASH_CHAT_API_KEY
    return response.request.newBuilder().header("Authorization", "Bearer $token").build()
  }
}