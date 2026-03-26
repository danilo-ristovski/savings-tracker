package com.savings.tracker.domain.usecase

import android.content.Context
import android.net.Uri
import com.savings.tracker.domain.model.ExportData
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import java.io.File
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(uri: Uri, pin: String) {
        val cacheDir = context.cacheDir
        val tempZip = File(cacheDir, "import_temp.zip")
        val extractDir = File(cacheDir, "import_extract")

        try {
            // Copy URI to temp file
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempZip.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Cannot open URI")

            // Clean extract dir
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            // Decrypt zip with Zip4j
            val zipFile = ZipFile(tempZip, pin.toCharArray())
            zipFile.extractAll(extractDir.absolutePath)

            // Find the JSON file
            val jsonFile = extractDir.listFiles()?.firstOrNull { it.extension == "json" }
                ?: throw IllegalArgumentException("No JSON file found in archive")

            val jsonString = jsonFile.readText()

            // Try ExportData first, fallback to bare List<Transaction> for v0
            val transactions: List<Transaction> = try {
                val exportData = json.decodeFromString(ExportData.serializer(), jsonString)
                exportData.transactions
            } catch (e: Exception) {
                json.decodeFromString(ListSerializer(Transaction.serializer()), jsonString)
            }

            repository.upsertTransactions(transactions)
        } finally {
            tempZip.delete()
            if (extractDir.exists()) extractDir.deleteRecursively()
        }
    }
}
