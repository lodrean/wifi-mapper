package com.wifimapper.domain.usecase

import com.wifimapper.domain.model.Session

interface ExportSessionUseCase {
    suspend operator fun invoke(session: Session): String
}
