package com.example.chatapp.di

import android.content.Context
import androidx.work.WorkManager
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ChatRepositoryImpl
import com.example.chatapp.data.repository.ModelFileRepository
import com.example.chatapp.domain.usecase.DownloadModelUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideChatRepository(repositoryImpl: ChatRepositoryImpl): ChatRepository = repositoryImpl

    @Provides
    @Singleton
    fun provideDownloadModelUseCase(
        @ApplicationContext context: Context,
        appPreferences: AppPreferences
    ): DownloadModelUseCase = DownloadModelUseCase(context, appPreferences)

    @Provides
    @Singleton
    fun provideModelFileRepository(@ApplicationContext context: Context): ModelFileRepository =
        ModelFileRepository(context)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

}
