package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import app.code.xfp.databinding.AccountPictureBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.util.*


class AccountPicture : AppCompatActivity() {
    private lateinit var binding : AccountPictureBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    var img = false
    private var imgpath : Uri = "".toUri()
    private var imagepath : String = ""
    private var uid:String = ""

    private val GALLERY_REQUEST_CODE = 1234
    private val PICK_IMAGE_REQUEST_CODE = 1
    private var mCropImageUri: Uri? = null
    private var dialog : Dialog? = null
    private var userId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUser()

        binding = AccountPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle : Bundle?= intent.extras
        userId = bundle?.getString("userId")

        if (userId.isNullOrEmpty()){
            userId = auth.currentUser!!.uid
        }



        getImage()
        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_wait)
        dialog!!.setCanceledOnTouchOutside(false)

        binding.uploadpic.setOnClickListener{
            pickImageFromGallery()
        }
        binding.updatePic.setOnClickListener{
            uploadImageToFirebase(imgpath)
        }

    }



    private fun pickImageFromGallery() {

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, Register.IMAGE_REQUEST_CODE)
    }

    //Method that will start after user picks image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Register.IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            val file_uri = data.data
            if (file_uri != null) {
                img = true
                imgpath = file_uri

                Glide.with(this)
                    .load(file_uri)
                    .into(binding.profileIcon)
            }
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {
        checkInternet()
        if (fileUri != null && img) {
            binding.uploadpic.isEnabled = false
            binding.updatePic.isEnabled = false
            dialog?.show()
            val fileName = "$userId.png"
            imagepath = fileName

            val refStorage = FirebaseStorage.getInstance().reference.child("profile_images/$fileName")

            val imageBitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
            val file = File(cacheDir, fileName)
            file.createNewFile()

            val outputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.WEBP, 65, outputStream)
            outputStream.flush()
            outputStream.close()
            val uri = Uri.fromFile(file)

            refStorage.putFile(uri)
                .addOnSuccessListener(
                    OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                            val imageUrl = it.toString()
                            dialog?.dismiss()
                            binding.uploadpic.isEnabled = true
                            binding.updatePic.isEnabled = true

                            if (userId != auth.currentUser!!.uid){
                                val notify = NotificationObj(auth.currentUser!!.uid,userId!!,"warning","Profile Picture Edit","Your profile picture has been edited by administrator !")
                                data.addNotification(notify)
                            }
                            file.delete()

                            StyleableToast.makeText(this, "Image updated!", Toast.LENGTH_SHORT, app.code.xfp.R.style.myToastUploaded).show();
                            switchBack()
                        }
                    })
                .addOnFailureListener(OnFailureListener { e ->
                    StyleableToast.makeText(this, "Error, try later !", Toast.LENGTH_SHORT,app.code.xfp.R.style.myToastFail).show();
                    dialog?.dismiss()
                    file.delete()
                    binding.uploadpic.isEnabled = true
                    binding.updatePic.isEnabled = true
                })
        }
        else{
            StyleableToast.makeText(this, "Select Image first!", Toast.LENGTH_SHORT, app.code.xfp.R.style.myToastFail).show();
        }
    }

    private fun getImage(){
        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${userId}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.profileIcon.loadImage(bitmap)
        }.addOnFailureListener{
            binding.profileIcon.loadImage(R.drawable.person)
        }
    }

    private fun switchBack(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }


    private fun checkDelete(){
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    if (user!!.name == "DELETED USER" && user.ballance == -1){
                        val intent = Intent(applicationContext, NoAccount::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener)
    }


    private fun checkInternet() {
        if (!isNetworkAvailable()) {
            StyleableToast.makeText(this, "No Connection!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            val intent = Intent(this, NoConnection::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }


    //Check if user is logged in
    private fun checkUser(){
        checkInternet()
        val user = auth.currentUser

        if (user == null){ //If he's not (switch to login activity)
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        else{
            checkDelete()
        }
    }

    override fun onStart() {
        super.onStart()
        checkUser()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }
}