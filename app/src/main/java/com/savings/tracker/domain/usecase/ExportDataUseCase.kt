package com.savings.tracker.domain.usecase

import android.content.Context
import android.net.Uri
import com.savings.tracker.domain.model.ExportData
import com.savings.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true }

    private suspend fun buildJsonString(): String {
        val transactions = repository.getAllTransactionsList()
        val exportData = ExportData(version = 1, transactions = transactions)
        return json.encodeToString(ExportData.serializer(), exportData)
    }

    suspend fun exportZip(destinationUri: Uri, pin: String) {
        val jsonString = buildJsonString()

        val cacheDir = context.cacheDir
        val jsonFile = File(cacheDir, "savings_backup.json")
        jsonFile.writeText(jsonString)

        val tempZip = File(cacheDir, "savings_backup.zip")
        if (tempZip.exists()) tempZip.delete()

        val zipFile = ZipFile(tempZip, pin.toCharArray())
        val zipParameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }
        zipFile.addFile(jsonFile, zipParameters)
        zipFile.close()
        jsonFile.delete()

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            tempZip.inputStream().use { it.copyTo(outputStream) }
        } ?: throw IllegalStateException("Could not open output stream")

        tempZip.delete()
    }
}
