package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import app.code.xfp.databinding.CreateOfferBinding
import app.code.xfp.objects.Offer
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CreateOffer : AppCompatActivity() {
    private lateinit var binding: CreateOfferBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var data : FirebaseData
    var img = false
    private var imgpath : Uri = "".toUri()
    private var imagepath : String = ""
    private var uid:String = ""
    private var dialog : Dialog? = null

    companion object {
        val IMAGE_REQUEST_CODE = 1_000;
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        binding = CreateOfferBinding.inflate(layoutInflater)

        auth = FirebaseAuth.getInstance()
        data = FirebaseData()

        //Check if user is logged in
        checkUser()

        setContentView(binding.root)
        getUserID()

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_wait)
        dialog!!.setCanceledOnTouchOutside(false)

        binding.uploadpic.setOnClickListener{
            pickImageFromGallery()
        }


        binding.createOffer.setOnClickListener{
            val title = binding.textInputEditTextO1.text.toString()
            val price = binding.textInputEditTextO2.text.toString()
            var shortDesc = binding.textInputEditTextO3.text.toString()
            var description = binding.textInputEditTextO4.text.toString()

            shortDesc = shortDesc.replace("\n", "<br>")
            description = description.replace("\n", "<br>")

            if (title.isEmpty() || price.isEmpty() || shortDesc.isEmpty() || description.isEmpty() || imgpath.toString().isEmpty()){
                StyleableToast.makeText(this, "All values must be entered!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else if (title.length < 6 && title.isNotEmpty()){
                binding.textInputEditTextO1.requestFocus()
                StyleableToast.makeText(this, "Title has to be at least 6!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else if (Integer.parseInt(price) <= 0 && price.isNotEmpty()){
                binding.textInputEditTextO2.requestFocus()
                StyleableToast.makeText(this, "Price has to be more than 0!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else if (shortDesc.isNotEmpty() && shortDesc.length < 12){
                binding.textInputEditTextO3.requestFocus()
                StyleableToast.makeText(this, "Short description has to be at least 12!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else if (description.isNotEmpty() && description.length < 12){
                binding.textInputEditTextO4.requestFocus()
                StyleableToast.makeText(this, "Description has to be at least 12!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else if (imgpath.toString().isEmpty()){
                binding.uploadpic.requestFocus()
                StyleableToast.makeText(this, "You have to enter image!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
            else {
                dialog?.show()
                binding.createOffer.isEnabled = false
                uploadImageToFirebase(imgpath)
                val offer = Offer(title,Integer.parseInt(price),shortDesc,description,imagepath,uid,0f,"",true)
                createOffer(offer,imagepath.toString().dropLast(4))
            }
        }

    }


    //Method for opening user gallery
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
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
                    .into(binding.imageView)
            }
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {
        if (fileUri != null) {
            dialog?.show()
            val fileName = UUID.randomUUID().toString() +".png"
            imagepath = fileName

            val refStorage = FirebaseStorage.getInstance().reference.child("offer_images/$fileName")

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
                            dialog?.dismiss()
                            StyleableToast.makeText(this, "Picture Uploaded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show();
                            file.delete()

                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    })
                .addOnFailureListener(OnFailureListener { e ->
                    dialog?.dismiss()
                    binding.createOffer.isEnabled = true
                    StyleableToast.makeText(this, "Issue with Uploading!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show();
                    file.delete()
                })
        }
    }

    private fun getUserID() {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    uid =  user!!.id
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener)
    }


    private fun createOffer(p: Offer, autoID : String){
        p.id = autoID
        data.addOffer(p).addOnSuccessListener {
            StyleableToast.makeText(this, "Offer Created & Uploading picture", Toast.LENGTH_SHORT, R.style.myToastDone).show();
        }
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
        checkDelete()

        if (user == null){ //If he's not (switch to login activity)
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    override fun onStart() {
        super.onStart()
        checkUser()
    }

    override fun onResume() {
        super.onResume()
        checkUser()
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }
}