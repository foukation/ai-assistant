package com.ai.multimodal

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ai.multimodal.business.MultimodalAssistant
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultimodalActivity : Activity() {
    private var currentPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置窗口为半屏显示，占据上半部分，背景为蓝色
        /*setupHalfScreenWindow(this, 0.50f, Color.BLUE)*/
        setupHalfScreenWindow(this, 0.50f, Color.TRANSPARENT)

        setContentView(R.layout.multimodal_main)

        showImagePickerDialog(
            this,
            { _: View? -> handleTakePhoto() },  // 拍照的逻辑
            { _: View? -> handlePickFromGallery() } // 从相册选择的逻辑
        )

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGallery()
            } else {
                Toast.makeText(this, "需要存储权限才能访问相册", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理返回的图片结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // 处理拍照返回的图片
                processImage(currentPhotoUri)
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                // 处理从相册选择的图片
                val selectedImageUri = data.data
                processImage(selectedImageUri)
            }
            return
        }
        if (resultCode == RESULT_CANCELED) {

            this.finish()

            return
        }
    }

    /**
     * 显示图片选择对话框
     *
     * @param context                上下文
     * @param onTakePhotoClick       拍照按钮点击回调
     * @param onSelectFromAlbumClick 从相册选择按钮点击回调
     */
    private fun showImagePickerDialog(
        context: Context, onTakePhotoClick: View.OnClickListener?,
        onSelectFromAlbumClick: View.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.image_multimodal_dialog, null)
        val dialog = builder.create()

        // 设置点击对话框外部区域时的回调
        dialog.setOnCancelListener {
            // 这里处理点击外部区域取消对话框的逻辑
            this.finish() // 如果需要结束当前Activity
        }

        // 或者使用setOnDismissListener捕获所有对话框消失的情况
        dialog.setOnDismissListener {
            // 这里处理对话框消失的逻辑（包括点击外部区域、返回键或手动调用dismiss）
            // 注意：这会捕获所有dismiss事件，包括按钮点击后的dismiss
            //this.finish()
        }

        dialog.show()
        dialog.window?.setContentView(dialogView)

        // 拍照按钮
        dialogView.findViewById<View>(R.id.bt_take_photo).setOnClickListener { view: View? ->
            onTakePhotoClick?.onClick(view)
            dialog.dismiss()
        }

        // 从相册选择按钮
        dialogView.findViewById<View>(R.id.bt_select_from_album).setOnClickListener { view: View? ->
            onSelectFromAlbumClick?.onClick(view)
            dialog.dismiss()
        }

        // 取消按钮
        dialogView.findViewById<View>(R.id.bt_image_cancel)
            .setOnClickListener { _: View? ->
                run {
                    this.finish()
                    dialog.dismiss()
                }
            }
    }


    /**
     * 处理拍照逻辑
     */
    private fun handleTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            launchCamera()
        }
    }

    /**
     * 启动相机应用
     */
    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // 创建保存照片的文件
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show()
            return
        }

        // 如果文件创建成功，继续
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",  // 确保在AndroidManifest中配置了FileProvider
                photoFile
            )

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)

            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "未找到相机应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 创建保存图片的文件
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 创建图片文件名
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // 创建文件
        return File.createTempFile(
            imageFileName,  // 前缀
            ".jpg",  // 后缀
            storageDir // 目录
        )
    }

    /**
     * 处理从相册选择图片的逻辑
     */
    private fun handlePickFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            launchGallery()
        }
    }

    /**
     * 启动图库应用
     */
    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.setType("image/*")
        try {
            startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "未找到图库应用", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 处理选取的图片
     *
     * @param imageUri 图片URI
     */
    private fun processImage(imageUri: Uri?) {
        if (imageUri != null) {
            // 这里处理获取到的图片
            // 例如: 显示图片、上传图片或进行图像处理等
            /*try {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver, imageUri
                )

                // 使用位图，例如设置到ImageView
                // imageView.setImageBitmap(bitmap);

                // 或者仅显示图片路径
                Toast.makeText(this, "图片已选择: $imageUri", Toast.LENGTH_SHORT).show()
                val drugInformationService = DrugInformationService()
                drugInformationService.menuTranslation(
                    this.applicationContext,
                    imageUri,  // 只提供图片路径，其他参数使用默认值
                    onSuccess = { response: ImageAssistantResponse ->
                        // 使用 Java 8 Lambda 处理成功响应
                        println("成功: " + response.content)
                        Unit // Kotlin 的 Unit 相当于 Java 的 void
                    },
                    onError = { error: String ->
                        // 使用 Java 8 Lambda 处理错误
                        println("错误: $error")
                        Unit
                    }
                )
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show()
            }*/
            val broadcastIntent = Intent("com.ai.multimodal.imageUriResponse")
            broadcastIntent.putExtra("imageUri", imageUri.toString())
            LocalBroadcastManager.getInstance(this.applicationContext)
                .sendBroadcast(broadcastIntent)
            MultimodalAssistant.imageUri = imageUri
            this.finish()
        }
    }

    companion object {
        // 在类成员变量区域添加
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_PICK_IMAGE = 1002
        private const val REQUEST_CAMERA_PERMISSION = 2001
        private const val REQUEST_STORAGE_PERMISSION = 2002

        /**
         * 设置窗口为半屏显示并允许底部区域接收触摸事件
         *
         * @param activity        当前Activity
         * @param heightRatio     上部区域占整个屏幕的高度比例(0.0-1.0)
         * @param backgroundColor 背景颜色
         */
        fun setupHalfScreenWindow(activity: Activity, heightRatio: Float, backgroundColor: Int) {
            // 设置窗口参数
            val window = activity.window
            val params = window.attributes

            // 获取屏幕尺寸
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)

            // 设置宽度为全屏
            params.width = WindowManager.LayoutParams.MATCH_PARENT

            // 设置高度 - 只覆盖部分区域
            val bottomHeight = (displayMetrics.heightPixels * heightRatio).toInt()
            params.height = displayMetrics.heightPixels - bottomHeight

            // 设置位置为顶部
            params.gravity = Gravity.TOP

            // 设置背景颜色
            window.setBackgroundDrawable(ColorDrawable(backgroundColor))

            // 关键：设置FLAG_NOT_TOUCH_MODAL，使得Activity下方的部分能接收触摸事件
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            // 如果希望只有Activity区域接收事件，其他部分不接收
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            // 应用参数
            window.attributes = params
        }
    }
}