package com.savings.tracker.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.savings.tracker.domain.model.Transaction
import com.savings.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(pin: String): Uri {
        val transactions = repository.getAllTransactionsList()

        val jsonString = json.encodeToString(
            ListSerializer(Transaction.serializer()),
            transactions
        )

        val cacheDir = context.cacheDir
        val jsonFile = File(cacheDir, "savings_export.json")
        jsonFile.writeText(jsonString)

        val zipFilePath = File(cacheDir, "savings_export.zip")
        if (zipFilePath.exists()) {
            zipFilePath.delete()
        }

        val zipFile = ZipFile(zipFilePath, pin.toCharArray())
        val zipParameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
        }
        zipFile.addFile(jsonFile, zipParameters)

        jsonFile.delete()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFilePath
        )
    }
}
