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

/** [AppPreferences][com.slygames.facade.data.local.datastore.AppPreferences] store: theming and overlay toggles. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppDataStore

/** [WallpaperPreferences][com.slygames.facade.data.local.datastore.WallpaperPreferences] store: selected live-wallpaper media. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WallpaperDataStore

private const val APP_PREFERENCES_NAME = "launcher_preferences"
private const val WALLPAPER_PREFERENCES_NAME = "wallpaper_preferences"

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_PREFERENCES_NAME
)

private val Context.wallpaperPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = WALLPAPER_PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @AppDataStore
    fun provideAppPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appPreferencesDataStore

    @Provides
    @Singleton
    @WallpaperDataStore
    fun provideWallpaperPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.wallpaperPreferencesDataStore
}
