package com.hairstyle.generator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import java.io.*

/**
 * 图片处理工具类
 */
object ImageUtils {

    /**
     * 将Uri转换为File
     */
    fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "temp_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

    /**
     * 将Bitmap保存为File
     */
    fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    /**
     * 将图片裁剪为1:1正方形（居中裁剪）
     */
    fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width == height) {
            return bitmap
        }
        val size = minOf(width, height)
        val x = (width - size) / 2
        val y = (height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * 压缩图片
     */
    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * 从文件路径加载Bitmap
     */
    fun loadBitmapFromFile(filePath: String, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            var inSampleSize = 1
            if (imageHeight > maxHeight || imageWidth > maxWidth) {
                val halfHeight = imageHeight / 2
                val halfWidth = imageWidth / 2

                while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(filePath, options)

            // 处理图片旋转
            val correctedBitmap = correctImageOrientation(bitmap, filePath)

            correctedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 修正图片方向
     */
    private fun correctImageOrientation(bitmap: Bitmap?, filePath: String): Bitmap? {
        if (bitmap == null) return null

        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return bitmap
        }
    }

    /**
     * 创建图片文件
     */
    fun createImageFile(context: Context, prefix: String = "IMG"): File {
        val timeStamp = System.currentTimeMillis()
        val imageFileName = "${prefix}_$timeStamp"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * 保存Bitmap到指定路径
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 计算图片的inSampleSize
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 从Uri加载缩放的Bitmap
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val newInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
            newInputStream?.close()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}