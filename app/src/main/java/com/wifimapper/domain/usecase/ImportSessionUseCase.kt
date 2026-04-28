package com.wifimapper.domain.usecase

import com.wifimapper.domain.model.Session

interface ImportSessionUseCase {
    suspend operator fun invoke(json: String): Session
}
