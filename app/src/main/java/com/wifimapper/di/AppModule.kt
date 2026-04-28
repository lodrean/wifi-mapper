package com.wifimapper.di

import android.content.Context
import androidx.room.Room
import com.wifimapper.data.database.AppDatabase
import com.wifimapper.data.export.ExportSessionUseCaseImpl
import com.wifimapper.data.export.ImportSessionUseCaseImpl
import com.wifimapper.data.repository.SensorTrackingRepositoryImpl
import com.wifimapper.data.repository.SessionRepositoryImpl
import com.wifimapper.data.repository.WifiScanRepositoryImpl
import com.wifimapper.domain.repository.SensorTrackingRepository
import com.wifimapper.domain.repository.SessionRepository
import com.wifimapper.domain.repository.WifiScanRepository
import com.wifimapper.domain.usecase.CreateSessionUseCase
import com.wifimapper.domain.usecase.DeleteSessionUseCase
import com.wifimapper.domain.usecase.ExportSessionUseCase
import com.wifimapper.domain.usecase.GetSessionsUseCase
import com.wifimapper.domain.usecase.ImportSessionUseCase
import com.wifimapper.presentation.home.HomeViewModel
import com.wifimapper.presentation.map.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(context: Context) {
    startKoin {
        androidContext(context)
        modules(appModule)
    }
}

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "wifi_mapper.db"
        ).build()
    }
    single { get<AppDatabase>().sessionDao() }

    // Repositories
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<WifiScanRepository> { WifiScanRepositoryImpl(androidContext()) }
    single<SensorTrackingRepository> { SensorTrackingRepositoryImpl(androidContext()) }

    // Use Cases
    factory { CreateSessionUseCase(get()) }
    factory { GetSessionsUseCase(get()) }
    factory { DeleteSessionUseCase(get()) }
    single<ExportSessionUseCase> { ExportSessionUseCaseImpl() }
    single<ImportSessionUseCase> { ImportSessionUseCaseImpl() }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), androidContext()) }
    viewModel { MapViewModel(androidContext(), get(), get(), get(), get()) }
}
