package com.wifimapper.presentation.home

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifimapper.domain.model.Session
import com.wifimapper.domain.repository.SessionRepository
import com.wifimapper.domain.usecase.DeleteSessionUseCase
import com.wifimapper.domain.usecase.ExportSessionUseCase
import com.wifimapper.domain.usecase.GetSessionsUseCase
import com.wifimapper.domain.usecase.ImportSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(
    private val getSessionsUseCase: GetSessionsUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val importSessionUseCase: ImportSessionUseCase,
    private val sessionRepository: SessionRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        getSessionsUseCase()
            .onEach { sessions ->
                _state.value = _state.value.copy(sessions = sessions, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnCreateSession -> { /* handled by navigation */ }
            is HomeAction.OnDeleteSession -> deleteSession(action.id)
            is HomeAction.OnExportSession -> exportSession(action.session)
            is HomeAction.OnImportSession -> importSession(action.json)
        }
    }

    private fun deleteSession(id: String) {
        viewModelScope.launch {
            deleteSessionUseCase(id)
        }
    }

    private fun exportSession(session: Session) {
        viewModelScope.launch {
            try {
                val json = exportSessionUseCase(session)
                val file = File(context.cacheDir, "wifi_map_${session.id}.json")
                file.writeText(json)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, session.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Export WiFi Map")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun importSession(json: String) {
        viewModelScope.launch {
            try {
                val session = importSessionUseCase(json)
                sessionRepository.updateSession(session)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
