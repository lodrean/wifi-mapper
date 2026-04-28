package com.wifimapper.data.export

import com.wifimapper.domain.model.Session
import com.wifimapper.domain.usecase.ImportSessionUseCase
import kotlinx.serialization.json.Json

class ImportSessionUseCaseImpl : ImportSessionUseCase {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun invoke(jsonString: String): Session {
        val dto = json.decodeFromString<MapExportDto>(jsonString)
        return dto.toDomainModel()
    }
}
