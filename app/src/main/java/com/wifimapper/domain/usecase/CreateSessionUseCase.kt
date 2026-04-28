package com.wifimapper.domain.usecase

import com.wifimapper.domain.repository.SessionRepository

open class CreateSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(name: String, stepLengthMeters: Float): String {
        return sessionRepository.createSession(name, stepLengthMeters)
    }
}
