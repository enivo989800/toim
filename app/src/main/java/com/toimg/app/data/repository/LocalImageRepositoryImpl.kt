package com.toimg.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.toimg.app.core.util.FileMoveUtil
import com.toimg.app.core.util.Sha256Util
import com.toimg.app.data.local.ToImgDatabase
import com.toimg.app.data.local.entity.FolderEntity
import com.toimg.app.data.local.entity.ImageEntity
import com.toimg.app.data.local.entity.RecognitionEntity
import com.toimg.app.data.local.entity.SmartRuleEntity
import com.toimg.app.domain.model.FolderItem
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.RecognitionResult
import com.toimg.app.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalImageRepositoryImpl : ImageRepository {

    private val prefName = "toimg_authorized_folders"
    private val keyFolderList = "folders_list"

    override fun observeImagesFromDb(context: Context): Flow<List<ImageItem>> {
        val db = ToImgDatabase.getInstance(context)
        return db.imageDao().getAllImages().map { entities ->
            entities.map { entity ->
                ImageItem(
                    id = entity.id,
                    uri = Uri.parse(entity.uriString),
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    dateModified = entity.dateModified,
                    mimeType = entity.mimeType,
                    folderName = entity.folderName,
                    folderPath = entity.folderPath,
                    sha256 = entity.sha256,
                    status = entity.status
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeDatabaseStats(context: Context): Flow<Pair<Int, Int>> {
        val db = ToImgDatabase.getInstance(context)
        return combine(db.imageDao().getImageCount(), db.imageDao().getHashedCount()) { total, hashed ->
            Pair(total, hashed)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun computeMissingHashes(context: Context, onProgress: (Int, Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val unhashed = db.imageDao().getImagesWithoutHash()
            val total = unhashed.size

            unhashed.forEachIndexed { index, entity ->
                val uri = Uri.parse(entity.uriString)
                val hash = Sha256Util.calculateSha256(context, uri)
                if (hash != null) {
                    db.imageDao().updateSha256(entity.id, hash)
                }
                onProgress(index + 1, total)
            }
        }
    }

    override fun scanMediaStoreImages(context: Context): Flow<List<ImageItem>> = flow {
        val imageList = mutableListOf<ImageItem>()
        val entityList = mutableListOf<ImageEntity>()
        val contentResolver: ContentResolver = context.contentResolver
        val db = ToImgDatabase.getInstance(context)

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "IMG_$id.jpg"
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn)
                    val mimeType = cursor.getString(mimeColumn) ?: "image/*"
                    val folderName = if (bucketColumn >= 0) cursor.getString(bucketColumn) ?: "图库" else "图库"
                    val path = if (pathColumn >= 0) cursor.getString(pathColumn) ?: "" else ""

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val item = ImageItem(
                        id = id.toString(),
                        uri = contentUri,
                        name = name,
                        path = path,
                        size = size,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        folderName = folderName,
                        folderPath = path
                    )

                    val entity = ImageEntity(
                        id = id.toString(),
                        uriString = contentUri.toString(),
                        name = name,
                        path = path,
                        size = size,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        folderName = folderName,
                        folderPath = path
                    )

                    imageList.add(item)
                    entityList.add(entity)

                    if (imageList.size % 20 == 0) {
                        db.imageDao().insertImages(entityList)
                        emit(imageList.toList())
                    }
                }
            }

            if (entityList.isNotEmpty()) {
                db.imageDao().insertImages(entityList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emit(imageList)
    }.flowOn(Dispatchers.IO)

    override fun scanFolderUri(context: Context, treeUri: Uri): Flow<List<ImageItem>> = flow {
        val imageList = mutableListOf<ImageItem>()
        val entityList = mutableListOf<ImageEntity>()
        val db = ToImgDatabase.getInstance(context)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@flow

        val folderName = rootDoc.name ?: "SAF目录"
        traverseDocumentTree(context, rootDoc, folderName, imageList, entityList) { batchItems, batchEntities ->
            db.imageDao().insertImages(batchEntities)
            emit(batchItems)
        }

        if (entityList.isNotEmpty()) {
            db.imageDao().insertImages(entityList)
        }
        emit(imageList)
    }.flowOn(Dispatchers.IO)

    private suspend fun traverseDocumentTree(
        context: Context,
        dir: DocumentFile,
        folderName: String,
        itemAccumulator: MutableList<ImageItem>,
        entityAccumulator: MutableList<ImageEntity>,
        onProgress: suspend (List<ImageItem>, List<ImageEntity>) -> Unit
    ) {
        val files: Array<DocumentFile> = dir.listFiles()
        files.forEach { file ->
            if (file.isDirectory) {
                traverseDocumentTree(context, file, file.name ?: folderName, itemAccumulator, entityAccumulator, onProgress)
            } else if (file.isFile && (file.type?.startsWith("image/") == true || isImageExtension(file.name))) {
                val item = ImageItem(
                    id = file.uri.toString(),
                    uri = file.uri,
                    name = file.name ?: "Image",
                    path = file.uri.path ?: "",
                    size = file.length(),
                    dateModified = file.lastModified() / 1000,
                    mimeType = file.type ?: "image/jpeg",
                    folderName = folderName,
                    folderPath = dir.uri.path ?: ""
                )
                val entity = ImageEntity(
                    id = file.uri.toString(),
                    uriString = file.uri.toString(),
                    name = file.name ?: "Image",
                    path = file.uri.path ?: "",
                    size = file.length(),
                    dateModified = file.lastModified() / 1000,
                    mimeType = file.type ?: "image/jpeg",
                    folderName = folderName,
                    folderPath = dir.uri.path ?: ""
                )
                itemAccumulator.add(item)
                entityAccumulator.add(entity)

                if (itemAccumulator.size % 10 == 0) {
                    onProgress(itemAccumulator.toList(), entityAccumulator.toList())
                }
            }
        }
    }

    private fun isImageExtension(fileName: String?): Boolean {
        if (fileName == null) return false
        val lower = fileName.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".heic") || lower.endsWith(".bmp")
    }

    override fun getSavedFolders(context: Context): List<FolderItem> {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val rawSet = prefs.getStringSet(keyFolderList, emptySet()) ?: emptySet()
        val persistedPermissions = context.contentResolver.persistedUriPermissions

        return rawSet.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) {
                val uriString = parts[0]
                val name = parts[1]
                val uri = Uri.parse(uriString)
                val isAuth = persistedPermissions.any { it.uri == uri && it.isReadPermission }
                FolderItem(
                    id = uriString,
                    name = name,
                    uri = uri,
                    isAuthorized = isAuth
                )
            } else null
        }
    }

    override fun saveAuthorizedFolder(context: Context, uri: Uri, folderName: String) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(keyFolderList, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add("${uri}|$folderName")
        prefs.edit().putStringSet(keyFolderList, currentSet).apply()

        // Persist Folder entity into Room DB
        val db = ToImgDatabase.getInstance(context)
        val folderEntity = FolderEntity(
            id = uri.toString(),
            name = folderName,
            uriString = uri.toString(),
            isAuthorized = true
        )
        // Store in DB in background thread
        Thread {
            try {
                db.openHelper.writableDatabase
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun removeAuthorizedFolder(context: Context, uriString: String) {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(keyFolderList, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.removeAll { it.startsWith("$uriString|") }
        prefs.edit().putStringSet(keyFolderList, currentSet).apply()
    }

    override fun observeRecognitions(context: Context): Flow<List<RecognitionResult>> {
        val db = ToImgDatabase.getInstance(context)
        return db.recognitionDao().getAllRecognitions().map { entities ->
            entities.map { entity ->
                val kwList = entity.keywords.split(",", "，")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                RecognitionResult(
                    imageId = entity.imageId,
                    keywords = kwList,
                    description = entity.description,
                    modelName = entity.modelName,
                    isSuccess = entity.status == "SUCCESS",
                    ocrText = entity.ocrText,
                    isSensitive = entity.isSensitive,
                    errorMessage = entity.errorMessage,
                    recognizedAt = entity.recognizedAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeSuccessfulRecognitions(context: Context): Flow<List<RecognitionResult>> {
        val db = ToImgDatabase.getInstance(context)
        return db.recognitionDao().getSuccessfulRecognitions().map { entities ->
            entities.map { entity ->
                val kwList = entity.keywords.split(",", "，")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                RecognitionResult(
                    imageId = entity.imageId,
                    keywords = kwList,
                    description = entity.description,
                    modelName = entity.modelName,
                    isSuccess = true,
                    ocrText = entity.ocrText,
                    isSensitive = entity.isSensitive,
                    errorMessage = null,
                    recognizedAt = entity.recognizedAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveRecognitions(context: Context, results: List<RecognitionResult>) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val entities = results.map { res ->
                RecognitionEntity(
                    imageId = res.imageId,
                    keywords = res.keywords.joinToString(", "),
                    description = res.description,
                    modelName = res.modelName,
                    status = if (res.isSuccess) "SUCCESS" else "FAILED",
                    ocrText = res.ocrText,
                    isSensitive = res.isSensitive,
                    errorMessage = res.errorMessage,
                    recognizedAt = res.recognizedAt
                )
            }
            db.recognitionDao().insertRecognitions(entities)

            // Update status in images table
            results.forEach { res ->
                val status = if (res.isSuccess) "RECOGNIZED" else "FAILED"
                db.imageDao().updateStatus(res.imageId, status)
            }
        }
    }

    override suspend fun getRecognition(context: Context, imageId: String): RecognitionResult? {
        return withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val entity = db.recognitionDao().getRecognitionByImageId(imageId) ?: return@withContext null
            val kwList = entity.keywords.split(",", "，")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            RecognitionResult(
                imageId = entity.imageId,
                keywords = kwList,
                description = entity.description,
                modelName = entity.modelName,
                isSuccess = entity.status == "SUCCESS",
                ocrText = entity.ocrText,
                isSensitive = entity.isSensitive,
                errorMessage = entity.errorMessage,
                recognizedAt = entity.recognizedAt
            )
        }
    }

    override suspend fun recordMoveSuccess(
        context: Context,
        imageId: String,
        newUriString: String,
        newName: String,
        newFolderName: String
    ) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            db.imageDao().updateImageLocation(
                id = imageId,
                uriString = newUriString,
                name = newName,
                folderName = newFolderName,
                status = "MOVED"
            )
        }
    }

    override suspend fun recordMoveHistory(
        context: Context,
        results: List<com.toimg.app.domain.model.MoveItemResult>,
        targetDirName: String
    ) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val historyEntities = results.map { item ->
                com.toimg.app.data.local.entity.MoveHistoryEntity(
                    imageId = item.imageId,
                    fileName = item.originalName,
                    sourcePath = item.sourceUri.toString(),
                    targetPath = item.targetUri?.toString() ?: "目标: $targetDirName",
                    status = item.status.name,
                    sha256 = item.targetHash ?: item.sourceHash,
                    executionMethod = item.executionMethod,
                    errorMessage = item.errorMessage,
                    timestamp = item.timestamp
                )
            }
            db.moveHistoryDao().insertHistory(historyEntities)
        }
    }

    override fun observeMoveHistory(context: Context): Flow<List<com.toimg.app.data.local.entity.MoveHistoryEntity>> {
        val db = ToImgDatabase.getInstance(context)
        return db.moveHistoryDao().getAllHistory().flowOn(Dispatchers.IO)
    }

    override suspend fun rollbackMove(context: Context, historyId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val historyItem = db.moveHistoryDao().getHistoryById(historyId)
                ?: return@withContext Result.failure(IllegalArgumentException("未找到移动历史记录: ID=$historyId"))
            FileMoveUtil.executeRollback(context, historyItem)
        }
    }

    override suspend fun updateKeywords(context: Context, imageId: String, newKeywords: List<String>) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val kwString = newKeywords.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
            db.recognitionDao().updateKeywords(imageId, kwString)
        }
    }

    override suspend fun mergeCategory(context: Context, oldCategory: String, targetCategory: String) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val recognitions = db.recognitionDao().getRecognitionsWithKeyword(oldCategory)
            recognitions.forEach { rec ->
                val keywords = rec.keywords.split(",", "，")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toMutableList()

                if (keywords.contains(oldCategory)) {
                    keywords.remove(oldCategory)
                    if (!keywords.contains(targetCategory)) {
                        keywords.add(targetCategory)
                    }
                    val updated = keywords.joinToString(", ")
                    db.recognitionDao().updateKeywords(rec.imageId, updated)
                }
            }
        }
    }

    override fun observeSmartRules(context: Context): Flow<List<SmartRuleEntity>> {
        val db = ToImgDatabase.getInstance(context)
        return db.smartRuleDao().getAllRules().flowOn(Dispatchers.IO)
    }

    override suspend fun saveSmartRule(context: Context, rule: SmartRuleEntity): Long {
        return withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            db.smartRuleDao().insertRule(rule)
        }
    }

    override suspend fun deleteSmartRule(context: Context, id: Long) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            db.smartRuleDao().deleteRuleById(id)
        }
    }

    override suspend fun updateRuleStatus(context: Context, id: Long, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            db.smartRuleDao().updateRuleStatus(id, isEnabled)
        }
    }

    override suspend fun seedPresetSmartRulesIfEmpty(context: Context) {
        withContext(Dispatchers.IO) {
            val db = ToImgDatabase.getInstance(context)
            val currentRules = db.smartRuleDao().getAllRules().first()
            if (currentRules.isEmpty()) {
                val presets = listOf(
                    SmartRuleEntity(
                        name = "财务报销归档",
                        keywordTrigger = "发票, 收据, 账单, 报销, 票据",
                        targetFolderName = "Pictures/财务报销",
                        targetTreeUri = "",
                        isEnabled = true
                    ),
                    SmartRuleEntity(
                        name = "私密证件标记与保护",
                        keywordTrigger = "身份证, 银行卡, 驾驶证, 护照, 户口本, 证件",
                        targetFolderName = "Pictures/证件专存",
                        targetTreeUri = "",
                        isEnabled = true
                    ),
                    SmartRuleEntity(
                        name = "旅行风光合辑",
                        keywordTrigger = "风景, 山川, 海洋, 日落, 建筑, 旅行, 景区",
                        targetFolderName = "Pictures/旅行风光",
                        targetTreeUri = "",
                        isEnabled = true
                    ),
                    SmartRuleEntity(
                        name = "美食日常记录",
                        keywordTrigger = "美食, 菜肴, 咖啡, 甜点, 聚餐, 餐厅",
                        targetFolderName = "Pictures/美食相册",
                        targetTreeUri = "",
                        isEnabled = true
                    )
                )
                db.smartRuleDao().insertRules(presets)
            }
        }
    }
}

