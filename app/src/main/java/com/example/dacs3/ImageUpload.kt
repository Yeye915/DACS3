package com.example.dacs3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors

/** Compresses selected images, closes streams and stores uploads under the signed-in user's UID. */
object ImageUpload {
    private val worker=Executors.newFixedThreadPool(2)
    fun upload(context: Context,uri: Uri): Task<String> {
        val uid=checkNotNull(Backend.auth.currentUser?.uid) { "Vui lòng đăng nhập để tải ảnh" }
        return Tasks.call(worker) {
            require(context.contentResolver.getType(uri)?.startsWith("image/")==true) { "Vui lòng chọn tệp ảnh" }
            val bytes=ByteArrayOutputStream().use { output ->
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    val buffer=ByteArray(8192);var total=0
                    while(true) { val read=input.read(buffer);if(read<0) break;total+=read
                        require(total<=10*1024*1024) { "Ảnh tối đa 10 MB" };output.write(buffer,0,read) }
                };output.toByteArray()
            }
            require(bytes.size<=10*1024*1024) { "Ảnh tối đa 10 MB" }
            val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            require(options.outWidth>0 && options.outHeight>0 && options.outWidth.toLong()*options.outHeight<=100_000_000) { "Ảnh không hợp lệ hoặc quá lớn" }
            options.inJustDecodeBounds=false;options.inSampleSize=1
            while(maxOf(options.outWidth,options.outHeight)/options.inSampleSize>1600) options.inSampleSize*=2
            val bitmap=requireNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)) { "Không đọc được ảnh" }
            try { ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,85,it);it.toByteArray() } } finally { bitmap.recycle() }
        }.continueWithTask { encoded ->
            val bytes=encoded.result
            val ref=Backend.storage.reference.child("users/$uid/uploads/${UUID.randomUUID()}.jpg")
            ref.putBytes(bytes,StorageMetadata.Builder().setContentType("image/jpeg").build())
                .continueWithTask { upload -> if(!upload.isSuccessful) throw upload.exception!!;ref.downloadUrl }
                .continueWith { download -> download.result.toString() }
        }
    }
}
