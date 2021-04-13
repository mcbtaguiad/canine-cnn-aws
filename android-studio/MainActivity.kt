package com.example.dogdiseaseidentifier

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_data.view.*
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern



class MainActivity : AppCompatActivity() {


   private val PERMISSION_CODE = 1000
   private val IMAGE_CAPTURE_CODE = 1001
   private val GALLERY_SELECT_CODE = 1002


   private lateinit var mClassifier: Classifier
   private lateinit var mBitmap: Bitmap
   private lateinit var mBitmapScaled: Bitmap


   private val mInputSize = 224
   private val mModelPath = "model_v1.tflite"
   private val mLabelPath = "labels.txt"


   lateinit var currentPhotoPath: String


   var selectedImagesPaths // Paths of the image(s) selected by the user.
           : ArrayList<String?>? = null
   var imagesSelected = false // Whether the user selected at least an image or not.6

   var cameraupload = false
   var galleryupload = false

   var cameradialog = false
   var gallerydialog = true




   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)


       requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
       mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)


       val btn = findViewById<Button>(R.id.btmenu) as Button

       btn.setOnClickListener {
           val popupMenu = PopupMenu(this, it)

           popupMenu.setOnMenuItemClickListener { item ->
               when (item.itemId){
                   R.id.cameramenu -> {
                       cameradialog = true
                       gallerydialog = false

                       if (isConnected()){
                           ShowAlertInputData()
                       }else{
                           openCamera()
                       }


                       //startActivity(intent)
                       true
                   }
                   R.id.gallerymenu -> {
                       cameradialog = false
                       gallerydialog = true

                       if (isConnected()){
                           ShowAlertInputData()
                       }else{
                           openGallery()
                       }

                       true
                   }
                   else -> false
               }
           }

           popupMenu.inflate(R.menu.popup_menu)

           try {
               val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
               fieldMPopup.isAccessible = true
               val mPopup = fieldMPopup.get(popupMenu)
               mPopup.javaClass
                   .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                   .invoke(mPopup, true)
           } catch (e: Exception){
               Log.e("Main", "Error showing menu icons.", e)
           } finally {
               popupMenu.show()
           }
       }

   }


   override fun onCreateOptionsMenu(menu: Menu): Boolean {
       val inflater = menuInflater
       inflater.inflate(R.menu.main_menu, menu) //your file name
       return super.onCreateOptionsMenu(menu)
   }


   override fun onOptionsItemSelected(item: MenuItem): Boolean {

       return when (item.itemId) {
           R.id.aboutapp -> {
               ShowAlertMainMenu()

               true
           }

           else -> super.onOptionsItemSelected(item)
       }
   }


   private fun ShowAlertMainMenu() {
       val builder = AlertDialog.Builder(this)
       builder.setTitle("About Application")
       builder.setMessage(
           "Mapua University Undergraduate Thesis\n" +
                   "Members: " + "Joshua Ang;\n" +
                   "Julie Seline Tapang;\n" +
                   "Jovan Valenzuela\n" +
                   "Advisers: " +
                   "Engr. Jessie Balbin;\n" +
                   "Engr. Julius Sese")
       builder.setPositiveButton("OK", null)
       val dialog = builder.show()
       val messageText = dialog.findViewById<View>(android.R.id.message) as TextView?
       messageText!!.gravity = Gravity.CENTER
       dialog.show()
   }



   private fun ShowAlertInputData() {

       if (cameradialog && !gallerydialog) {


           val builder = AlertDialog.Builder(this)
           builder.setTitle("Dog Details")

           // set the custom layout
           val customLayout: View = layoutInflater.inflate(R.layout.dialog_data, null)
           builder.setView(customLayout)


           // add a button
           builder
               .setPositiveButton("OK") { dialog, which -> // send data from the
                   // AlertDialog to the Activity


                   val breed = customLayout.edittextbreed.text.toString()
                   val agemonth = customLayout.edittextagemonth.text.toString()
                   val sex = customLayout.spinnersex.selectedItem.toString()

                   //val sumstrupload="breed="+breed+"&sex="+sex+"&agemonth="+agemonth
                   val sumstrupload=breed+"---"+sex+"---"+agemonth

                   openCamera()
                   sendDialogDataToActivity(sumstrupload)

                   val testtxt = findViewById<TextView>(R.id.ResultTextView1)

                   testtxt.text = ""

               }
               .setNegativeButton("CANCEL") { dialog, which ->
                   //Toast.makeText(applicationContext, "Cancel is pressed", Toast.LENGTH_LONG).show()
               }

           // create and show
           // the alert dialog
           val dialog = builder.create()
           dialog.show()
       }
       else if (!cameradialog && gallerydialog) {
           // Create an alert builder
           val builder = AlertDialog.Builder(this)
           builder.setTitle("Dog Details")

           // set the custom layout
           val customLayout: View = layoutInflater.inflate(R.layout.dialog_data, null)
           builder.setView(customLayout)

           // add a button
           builder
               .setPositiveButton("OK") { dialog, which -> // send data from the
                   // AlertDialog to the Activity

                   val breed = customLayout.edittextbreed.text.toString()
                   val agemonth = customLayout.edittextagemonth.text.toString()
                   val sex = customLayout.spinnersex.selectedItem.toString()


                   //val sumstrupload="breed="+breed+"&sex="+sex+"&agemonth="+agemonth
                   val sumstrupload=breed+"---"+sex+"---"+agemonth
                   openGallery()
                   sendDialogDataToActivity(sumstrupload)

                   val testtxt = findViewById<TextView>(R.id.ResultTextView1)

                   testtxt.text = ""


               }
               .setNegativeButton("CANCEL") { dialog, which ->
                   //Toast.makeText(applicationContext, "Cancel is pressed", Toast.LENGTH_LONG).show()
               }

           // create and show
           // the alert dialog
           val dialog = builder.create()
           dialog.show()

       }



   }

   // Do something with the data
   // coming from the AlertDialog
   private fun sendDialogDataToActivity(data: String) {
       //Toast.makeText(this, data, Toast.LENGTH_SHORT)
       //.show()
       val sumstr = data

       val testtxt = findViewById<TextView>(R.id.ResultTextView2)


       testtxt.text = sumstr


   }



   private fun ShowAlertUpload() {
       AlertDialog.Builder(this)
           .setTitle("Failed to upload dog details!")
           .setMessage("Try again?")
           .setPositiveButton("OK") { dialog, which ->
               connectServer()
               //Toast.makeText(applicationContext, "OK is pressed", Toast.LENGTH_LONG).show()
           }
           .setNegativeButton("CANCEL") { dialog, which ->
               //Toast.makeText(applicationContext, "Cancel is pressed", Toast.LENGTH_LONG).show()
           }

           .show()

   }




   private fun openCamera() {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
           if (checkSelfPermission(Manifest.permission.CAMERA)
               == PackageManager.PERMISSION_DENIED ||
               checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
               == PackageManager.PERMISSION_DENIED){
               //permission was not enabled
               val permission = arrayOf(
                   Manifest.permission.CAMERA,
                   Manifest.permission.WRITE_EXTERNAL_STORAGE
               )
               //show popup to request permission
               requestPermissions(permission, PERMISSION_CODE)
           }
           else{
               //permission already granted
               //cameraintent()
               dispatchTakePictureIntent()
           }
       }
       else{
           //system os is < marshmallow
           //cameraintent()
           dispatchTakePictureIntent()

       }
   }


   private fun dispatchTakePictureIntent() {
       Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
           // Ensure that there's a camera activity to handle the intent
           takePictureIntent.resolveActivity(packageManager)?.also {
               // Create the File where the photo should go
               val photoFile: File? = try {
                   createImageFile()
               } catch (ex: IOException) {
                   // Error occurred while creating the File
                   null
               }
               // Continue only if the File was successfully created
               photoFile?.also {
                   val photoURI: Uri = FileProvider.getUriForFile(
                       this,
                       "com.example.android.fileprovider",
                       it
                   )
                   takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                   startActivityForResult(takePictureIntent, IMAGE_CAPTURE_CODE)
               }
           }
       }
   }


   @Throws(IOException::class)
   private fun createImageFile(): File {
       // Create an image file name
       val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
       val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
       return File.createTempFile(
           "JPEG_${timeStamp}_", /* prefix */
           ".jpg", /* suffix */
           storageDir /* directory */
       ).apply {
           // Save a file: path for use with ACTION_VIEW intents
           currentPhotoPath = absolutePath
       }
   }

  

   private fun openGallery() {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
           if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
               == PackageManager.PERMISSION_DENIED ||
               checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
               == PackageManager.PERMISSION_DENIED){

               //permission was not enabled
               val permission = arrayOf(
                   Manifest.permission.WRITE_EXTERNAL_STORAGE,
                   Manifest.permission.READ_EXTERNAL_STORAGE
               )
               //show popup to request permission
               requestPermissions(permission, PERMISSION_CODE)
           }
           else{
               //permission already granted
               galleryintent()
           }
       }
       else{
           //system os is < marshmallow
           galleryintent()
       }

   }
  

   private fun galleryintent(){
       val intent = Intent()
       intent.type = "image/*"
       //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
       intent.action = Intent.ACTION_GET_CONTENT
       startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_SELECT_CODE)
   }

   override fun onRequestPermissionsResult(
       requestCode: Int,
       permissions: Array<out String>,
       grantResults: IntArray
   ) {
       //called when user presses ALLOW or DENY from Permission Request Popup
       when(requestCode){
           PERMISSION_CODE -> {
               if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   if (cameradialog) {
                       openCamera()
                   } else {
                       openGallery()
                   }
                   //permission from popup was granted
               } else {
                   //permission from popup was denied
                   Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
               }
           }
       }
   }
  


   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
       super.onActivityResult(requestCode, resultCode, data)

       val df = DecimalFormat("#.####")
       df.roundingMode = RoundingMode.CEILING

       when (requestCode) {
           IMAGE_CAPTURE_CODE -> {
               try {
                   if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {

                       val file = File(currentPhotoPath)
                       val photoBitmap = BitmapFactory.decodeFile(file.absolutePath)
                       //val croppedBitmap = getCroppedBitmap(photoBitmap)
                       //classifyAndShowResult(croppedBitmap)


                       galleryupload = false
                       imagesSelected = true
                       cameraupload = true


                       mBitmapScaled = scaleImage(photoBitmap)


                       //saveImage(photoBitmap)
                       PhotoImageView.setImageBitmap(photoBitmap)


                       //C L A S S I F F I E R
                      

                       if (isConnected()){
                           Toast.makeText(this, "Using Online Identifier.", Toast.LENGTH_LONG).show()

                           connectServer()
                       }else{
                           Toast.makeText(this, "Using Offline Identifier.", Toast.LENGTH_LONG).show()

                           val ResultView = findViewById<TextView>(R.id.ResultTextView1)
                           val results = mClassifier.recognizeImage(mBitmapScaled).firstOrNull()

                           //val accuracyresult = "Disease: " + results?.title + "\nConfidence: " + results?.confidence

                           val confidencestr = "" + results?.confidence
                           val accuracyresult = "Disease: " + results?.title + "\nConfidence: " + df.format(confidencestr.toFloat()*100) + "%"

                           ResultView.text = accuracyresult
                       }

                      

                   } else {

                       Toast.makeText(this, "Camera Cancelled", Toast.LENGTH_LONG).show()
                   }
               } catch (e: Exception) {
                   Toast.makeText(this, "Camera Cancelled.", Toast.LENGTH_LONG).show()
                   e.printStackTrace()
               }


           }
           GALLERY_SELECT_CODE -> {
               try {
                   if (requestCode == GALLERY_SELECT_CODE && resultCode == RESULT_OK && null != data) {
                       // When a single image is selected.
                       var currentImagePath: String?
                       selectedImagesPaths = java.util.ArrayList()
                      
                       if (data.data != null) {
                           val uri = data.data
                           try {
                               mBitmap = MediaStore.Images.Media.getBitmap(
                                   this.contentResolver,
                                   uri
                               )
                           } catch (e: IOException) {
                               e.printStackTrace()
                           }
                           currentImagePath = getPath(applicationContext, uri)
                           Log.d("ImageDetails", "Single Image URI : $uri")
                           Log.d("ImageDetails", "Single Image Path : $currentImagePath")
                           selectedImagesPaths!!.add(currentImagePath)
                           cameraupload = false
                           galleryupload = true
                           imagesSelected = true

                           mBitmapScaled = scaleImage(mBitmap)
                           PhotoImageView.setImageBitmap(mBitmap)
                          

                           //C L A S S I F F I E R

                           if (isConnected()){
                               Toast.makeText(this, "Using Online Identifier.", Toast.LENGTH_LONG).show()

                               connectServer()
                           }else{
                               Toast.makeText(this, "Using Offline Identifier.", Toast.LENGTH_LONG).show()

                               val ResultView = findViewById<TextView>(R.id.ResultTextView1)
                               val results = mClassifier.recognizeImage(mBitmapScaled).firstOrNull()

                              

                               val confidencestr = "" + results?.confidence
                               val accuracyresult = "Disease: " + results?.title + "\nConfidence: " + df.format(confidencestr.toFloat()*100) + "%"

                               ResultView.text = accuracyresult
                           }
                          
                       }
                   } else {
                       Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG)
                           .show()
                   }
                
               } catch (e: Exception) {
                   Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG).show()
                   e.printStackTrace()
               }
           }
           else -> {
               Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show()
           }
       }

   }
  

   fun scaleImage(bitmap: Bitmap?): Bitmap {
       val originalWidth = bitmap!!.width

       val originalHeight = bitmap.height
       val scaleWidth = mInputSize.toFloat() / originalWidth
       val scaleHeight = mInputSize.toFloat() / originalHeight
       val matrix = Matrix()
       matrix.postScale(scaleWidth, scaleHeight)
       return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
   }

   fun isConnected(): Boolean {
       var connected = false
       try {
           val cm =
               applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
           val nInfo = cm.activeNetworkInfo
           connected = nInfo != null && nInfo.isAvailable && nInfo.isConnected
           return connected
       } catch (e: Exception) {
           Log.e("Main", "Connectivity Exception", e)
       }
       return connected
   }

  
   private fun connectServer() {

       val uploadinfo: String = ResultTextView2.text.toString()

       if (cameraupload && !galleryupload){



           if (imagesSelected == false) {

               return
           }

           val file = File(currentPhotoPath)



           val postUrl = "http://ec2-52-221-198-194.ap-southeast-1.compute.amazonaws.com/" + "/postdata_tensor"


           val multipartBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

           val options = BitmapFactory.Options()
           options.inPreferredConfig = Bitmap.Config.RGB_565
           val stream = ByteArrayOutputStream()
           try {
               // Read BitMap by file path.
               val bitmap = BitmapFactory.decodeFile(file.absolutePath)
               bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
           } catch (e: Exception) {
    
               return
           }
           val byteArray = stream.toByteArray()

           multipartBodyBuilder.addFormDataPart(
               "image", uploadinfo + ".jpg", RequestBody.create(
                   MediaType.parse(
                       "image/*jpg"
                   ), byteArray
               )
           )

           val postBodyImage: RequestBody = multipartBodyBuilder.build()


           postRequest(postUrl, postBodyImage)

       }else if (!cameraupload && galleryupload){
          
 
           if (imagesSelected == false) {

               return
              

           val postUrl = "http://ec2-52-221-198-194.ap-southeast-1.compute.amazonaws.com/" + "/postdata_tensor"

           val multipartBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
           for (i in selectedImagesPaths!!.indices) {
               val options = BitmapFactory.Options()
               options.inPreferredConfig = Bitmap.Config.RGB_565
               val stream = ByteArrayOutputStream()
               try {
                   // Read BitMap by file path.
                   val bitmap = BitmapFactory.decodeFile(selectedImagesPaths!![i], options)
                   bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
               } catch (e: Exception) {
                   //responseText.text = "Please Make Sure the Selected File is an Image."
                   return
               }
               val byteArray = stream.toByteArray()

               multipartBodyBuilder.addFormDataPart(
                   "image", uploadinfo + ".jpg", RequestBody.create(
                       MediaType.parse(
                           "image/*jpg"
                       ), byteArray
                   )
               )
           }
           val postBodyImage: RequestBody = multipartBodyBuilder.build()


           postRequest(postUrl, postBodyImage)
       }
   }
      

   fun postRequest(postUrl: String?, postBody: RequestBody?) {

       val client = OkHttpClient()
       val request = Request.Builder()
           .url(postUrl)
           .post(postBody)
           .build()
       client.newCall(request).enqueue(object : Callback {
           override fun onFailure(call: Call, e: IOException) {

               call.cancel()
               Log.d("FAIL", e.message!!)


               runOnUiThread {

                   Toast.makeText(applicationContext, "Failed to Connect to Server", Toast.LENGTH_LONG).show()

                   ShowAlertUpload()

               }
           }


           @Throws(IOException::class)
           override fun onResponse(call: Call, response: Response) {


               runOnUiThread {
                   val responseText = findViewById<TextView>(R.id.ResultTextView1)

                   try {

                       responseText.text = response.body().string()

                   } catch (e: IOException) {
                       e.printStackTrace()
                   }
               }
           }
       })
   }
      

   companion object {
       private val IP_ADDRESS = Pattern.compile(
           "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                   + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                   + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                   + "|[1-9][0-9]|[0-9]))"
       )

       // Implementation of the getPath() method and all its requirements is taken from the StackOverflow Paul Burke's answer: https://stackoverflow.com/a/20559175/5426539
       fun getPath(context: Context, uri: Uri?): String? {
           val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

           // DocumentProvider
           if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
               // ExternalStorageProvider
               if (isExternalStorageDocument(uri)) {
                   val docId = DocumentsContract.getDocumentId(uri)
                   val split = docId.split(":".toRegex()).toTypedArray()
                   val type = split[0]
                   if ("primary".equals(type, ignoreCase = true)) {
                       return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                   }

                   // TODO handle non-primary volumes
               } else if (isDownloadsDocument(uri)) {
                   val id = DocumentsContract.getDocumentId(uri)
                   val contentUri = ContentUris.withAppendedId(
                       Uri.parse("content://downloads/public_downloads"),
                       java.lang.Long.valueOf(id)
                   )
                   return getDataColumn(context, contentUri, null, null)
               } else if (isMediaDocument(uri)) {
                   val docId = DocumentsContract.getDocumentId(uri)
                   val split = docId.split(":".toRegex()).toTypedArray()
                   val type = split[0]
                   var contentUri: Uri? = null
                   if (("image" == type)) {
                       contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                   } else if (("video" == type)) {
                       contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                   } else if (("audio" == type)) {
                       contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                   }
                   val selection = "_id=?"
                   val selectionArgs = arrayOf(
                       split[1]
                   )
                   return getDataColumn(context, contentUri, selection, selectionArgs)
               }
           } else if ("content".equals(uri!!.scheme, ignoreCase = true)) {
               return getDataColumn(context, uri, null, null)
           } else if ("file".equals(uri.scheme, ignoreCase = true)) {
               return uri.path
           }
           return null
       }

       fun getDataColumn(
           context: Context, uri: Uri?, selection: String?,
           selectionArgs: Array<String>?
       ): String? {
           var cursor: Cursor? = null
           val column = "_data"
           val projection = arrayOf(
               column
           )
           try {
               cursor = context.contentResolver.query(
                   (uri)!!, projection, selection, selectionArgs,
                   null
               )
               if (cursor != null && cursor.moveToFirst()) {
                   val column_index = cursor.getColumnIndexOrThrow(column)
                   return cursor.getString(column_index)
               }
           } finally {
               cursor?.close()
           }
           return null
       }

       fun isExternalStorageDocument(uri: Uri?): Boolean {
           return ("com.android.externalstorage.documents" == uri!!.authority)
       }

       fun isDownloadsDocument(uri: Uri?): Boolean {
           return ("com.android.providers.downloads.documents" == uri!!.authority)
       }

       fun isMediaDocument(uri: Uri?): Boolean {
           return ("com.android.providers.media.documents" == uri!!.authority)
       }
   }
      
}

