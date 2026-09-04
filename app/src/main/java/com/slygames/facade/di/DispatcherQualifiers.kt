package com.slygames.facade.di

import javax.inject.Qualifier

/** [kotlinx.coroutines.Dispatchers.IO] - disk/PackageManager/Room work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** [kotlinx.coroutines.Dispatchers.Default] - CPU-bound work (icon pack parsing, image compositing). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** [kotlinx.coroutines.Dispatchers.Main] - UI-thread work, injected rather than referenced directly for testability. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
