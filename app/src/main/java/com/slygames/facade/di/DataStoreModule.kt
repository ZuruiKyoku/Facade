package com.slygames.facade.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** [LauncherPreferences][com.slygames.facade.data.local.datastore.LauncherPreferences] store: grid size, icon scale, gestures. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LauncherDataStore

/** [WallpaperPreferences][com.slygames.facade.data.local.datastore.WallpaperPreferences] store: selected live-wallpaper media. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WallpaperDataStore

private const val LAUNCHER_PREFERENCES_NAME = "launcher_preferences"
private const val WALLPAPER_PREFERENCES_NAME = "wallpaper_preferences"

private val Context.launcherPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LAUNCHER_PREFERENCES_NAME
)

private val Context.wallpaperPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = WALLPAPER_PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @LauncherDataStore
    fun provideLauncherPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.launcherPreferencesDataStore

    @Provides
    @Singleton
    @WallpaperDataStore
    fun provideWallpaperPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.wallpaperPreferencesDataStore
}
