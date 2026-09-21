package com.drdisagree.iconify.data.common

import android.os.Environment
import com.drdisagree.iconify.BuildConfig

object Resources {

    // Preference files
    const val SHARED_XPREFERENCES = BuildConfig.APPLICATION_ID + "_xpreference"

    // Storage location
    val DOCUMENTS_DIR: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
    val DOWNLOADS_DIR: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

    val LOG_DIR = "$DOCUMENTS_DIR/Iconify"
    const val MODULE_DIR = "/data/adb/modules/Iconify"
    const val SYSTEM_OVERLAY_DIR = "/system/product/overlay"
    const val OVERLAY_DIR = "$MODULE_DIR/system/product/overlay"
    val BACKUP_DIR = Environment.getExternalStorageDirectory().absolutePath + "/.iconify_backup"
    val TEMP_DIR = Environment.getExternalStorageDirectory().absolutePath + "/.iconify"
    val TEMP_MODULE_DIR = "$TEMP_DIR/Iconify"
    val TEMP_MODULE_OVERLAY_DIR = "$TEMP_MODULE_DIR/system/product/overlay"
    val TEMP_OVERLAY_DIR = "$TEMP_DIR/overlays"
    val TEMP_CACHE_DIR = "$TEMP_OVERLAY_DIR/cache"
    val UNSIGNED_UNALIGNED_DIR = "$TEMP_OVERLAY_DIR/unsigned_unaligned"
    val UNSIGNED_DIR = "$TEMP_OVERLAY_DIR/unsigned"
    val SIGNED_DIR = "$TEMP_OVERLAY_DIR/signed"

    // File resources
    const val FRAMEWORK_DIR = "/system/framework/framework-res.apk"

    const val DYNAMIC_RESOURCE_DATABASE_NAME = "dynamic_resource_database"
    const val DYNAMIC_RESOURCE_TABLE = "dynamic_resource_table"
}
