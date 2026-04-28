package com.wifimapper.domain.usecase

import com.wifimapper.domain.repository.SessionRepository

class DeleteSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(id: String) = sessionRepository.deleteSession(id)
}
