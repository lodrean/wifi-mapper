package com.wifimapper.domain.usecase

import com.wifimapper.domain.model.Session
import com.wifimapper.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class GetSessionsUseCase(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<List<Session>> = sessionRepository.getAllSessions()
}
