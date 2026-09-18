package io.github.muntasimulhaque.quran.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.muntasimulhaque.quran.ui.settings.DataNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The two doors that move the reader's own saved work on and off the phone. */
data class SavedTransfer(
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onNoticeShown: () -> Unit,
)

/**
 * Export and import of the reader's saved ayahs and notes, through the
 * system's own file picker. The app writes a small JSON document and reads
 * one back; nothing leaves the device unless the reader chooses a file, and
 * an unreadable or foreign file is refused with one calm line.
 */
@Composable
fun rememberSavedTransfer(viewModel: ReaderViewModel): SavedTransfer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportSavedJson()
            if (json == null) {
                viewModel.reportDataNotice(DataNotice.NothingToExport)
                return@launch
            }
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
                }.getOrDefault(false)
            }
            viewModel.reportDataNotice(
                if (written) DataNotice.Exported else DataNotice.ImportFailed,
            )
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            viewModel.importSaved(text)
        }
    }

    return remember(viewModel, createDocument, openDocument) {
        SavedTransfer(
            onExport = {
                if (viewModel.hasSavedAyahs()) {
                    createDocument.launch(EXPORT_FILE_NAME)
                } else {
                    viewModel.reportDataNotice(DataNotice.NothingToExport)
                }
            },
            onImport = { openDocument.launch(arrayOf("*/*")) },
            onNoticeShown = { viewModel.reportDataNotice(null) },
        )
    }
}

private const val EXPORT_FILE_NAME = "quran-saved-ayahs.json"
