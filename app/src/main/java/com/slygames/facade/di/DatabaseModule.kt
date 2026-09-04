package com.slygames.facade.di

import android.content.Context
import androidx.room.Room
import com.slygames.facade.data.local.db.FacadeDatabase
import com.slygames.facade.data.local.db.dao.FolderDao
import com.slygames.facade.data.local.db.dao.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFacadeDatabase(@ApplicationContext context: Context): FacadeDatabase =
        Room.databaseBuilder(context, FacadeDatabase::class.java, FacadeDatabase.DATABASE_NAME)
            // Workspace layout is user data, not disposable cache - never destructively migrate
            // it. Real migrations should be added here as the schema evolves past v1.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    @Singleton
    fun provideWorkspaceDao(database: FacadeDatabase): WorkspaceDao = database.workspaceDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: FacadeDatabase): FolderDao = database.folderDao()
}
