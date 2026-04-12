package com.example.chatapp.di

import android.content.Context
import com.example.chatapp.data.preferences.AppPreferences
import com.example.chatapp.data.repository.ChatRepository
import com.example.chatapp.data.repository.ChatRepositoryImpl
import com.example.chatapp.domain.usecase.DownloadModelUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
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
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences = AppPreferences(context)
}
