package com.wifimapper.data.export

import com.wifimapper.domain.model.Session
import com.wifimapper.domain.usecase.ExportSessionUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportSessionUseCaseImpl : ExportSessionUseCase {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun invoke(session: Session): String {
        val dto = session.toExportDto()
        return json.encodeToString(dto)
    }
}
