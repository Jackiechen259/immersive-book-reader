package com.immersive.reader.di

import android.content.Context
import com.immersive.reader.BuildConfig
import com.immersive.reader.update.ApkDownloader
import com.immersive.reader.update.ApkInstaller
import com.immersive.reader.update.DataStoreUpdatePreferences
import com.immersive.reader.update.GitHubReleaseClient
import com.immersive.reader.update.HttpApkDownloader
import com.immersive.reader.update.HttpGitHubReleaseClient
import com.immersive.reader.update.UpdateCoordinator
import com.immersive.reader.update.UpdatePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {
    @Provides
    @Singleton
    fun provideGitHubReleaseClient(): GitHubReleaseClient =
        HttpGitHubReleaseClient(BuildConfig.VERSION_NAME)

    @Provides
    @Singleton
    fun provideApkDownloader(@ApplicationContext context: Context): ApkDownloader =
        HttpApkDownloader(context)

    @Provides
    @Singleton
    fun provideUpdatePreferences(@ApplicationContext context: Context): DataStoreUpdatePreferences =
        DataStoreUpdatePreferences(context)

    @Provides
    @Singleton
    fun provideUpdatePreferencesInterface(impl: DataStoreUpdatePreferences): UpdatePreferences = impl

    @Provides
    @Singleton
    fun provideApkInstaller(@ApplicationContext context: Context): ApkInstaller = ApkInstaller(context)

    @Provides
    @Singleton
    fun provideUpdateCoordinator(
        client: GitHubReleaseClient,
        downloader: ApkDownloader,
        preferences: UpdatePreferences,
    ): UpdateCoordinator = UpdateCoordinator(
        client = client,
        downloader = downloader,
        preferences = preferences,
        currentVersionName = BuildConfig.VERSION_NAME,
        ioDispatcher = Dispatchers.IO,
        clock = { System.currentTimeMillis() },
    )
}
