package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.code.xfp.databinding.OfferManageBinding
import app.code.xfp.objects.Message
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.Offer
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

class ManageOffer : AppCompatActivity() {
    private lateinit var binding: OfferManageBinding
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private var offerId = ""
    var img = false
    private var imgpath : Uri? = null
    private var imagepath : String = ""
    private var sellerId : String = ""
    private var dialog : Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OfferManageBinding.inflate(layoutInflater)
        checkInternet()

        auth = FirebaseAuth.getInstance()
        checkUser()

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_wait)
        dialog!!.setCanceledOnTouchOutside(false)

        //Getting data from Bundle and Setting them
        val bundle : Bundle?= intent.extras
        offerId = bundle!!.getString("id").toString()

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    val offer = data.getValue(Offer::class.java)

                    val desc = Html.fromHtml(offer!!.description)
                    val descS = Html.fromHtml(offer.smallDescription)
                    sellerId = offer.sellerId

                    binding.textInputEditTextF1.setText(offer.title)
                    binding.textInputEditTextF2.setText(offer.price.toString())
                    binding.textInputEditTextF3.setText(descS)
                    binding.textInputEditTextF4.setText(desc)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(offerId).addListenerForSingleValueEvent(listener)

        getPicture()

        binding.uploadpic.setOnClickListener {
            pickImageFromGallery()
        }

        binding.saveButtonOffer.setOnClickListener{
            update()
            if (imgpath != null) uploadImageToFirebase(imgpath!!)
        }

        binding.deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm Delete")
            builder.setMessage("Are you sure you want to Delete this offer ?")

            builder.setPositiveButton("Delete") { dialog, which ->
                deleteOffer()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        setContentView(binding.root)
    }

    private fun update(){
        val title = binding.textInputEditTextF1.text.toString()
        val price = binding.textInputEditTextF2.text.toString()
        var smallDescription = binding.textInputEditTextF3.text.toString()
        var description = binding.textInputEditTextF4.text.toString()

        description = description.replace("\n", "<br>")
        smallDescription = smallDescription.replace("\n", "<br>")

        val hashMap = HashMap<String, Any>()
        hashMap.put("title",title)
        hashMap.put("price",Integer.parseInt(price))
        hashMap.put("smallDescription",smallDescription)
        hashMap.put("description",description)

        dialog?.show()

        data.updateOffer(offerId,hashMap).addOnCompleteListener {
            if (it.isSuccessful) {
                dialog?.dismiss()
                if (sellerId != auth.currentUser!!.uid){
                    val notify = NotificationObj(auth.currentUser!!.uid,sellerId,"warning","Admin action","Your offer has been changed by admin!")
                    data.addNotification(notify)
                }
                StyleableToast.makeText(this, "Saved!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                if (imgpath == null) {
                    switchBack()
                }
            }
            else {
                dialog?.dismiss()
                StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        }

    }

    private fun switchBack(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun deleteOffer(){
        dialog?.show()
        data.getDatabaseReferenceOffer()!!.child(offerId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snapshot1 in snapshot.children){
                    val refStorage = FirebaseStorage.getInstance().reference.child("offer_images/$offerId.png")
                    refStorage.delete()
                    snapshot1.ref.removeValue()
                    dialog?.dismiss()
                    StyleableToast.makeText(applicationContext, "Offer Deleted!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                    switchBack()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                dialog?.dismiss()
                StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        })
    }

    private fun getPicture(){
        val storageReference = FirebaseStorage.getInstance().reference.child("offer_images/$offerId.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.imageView.setImageBitmap(bitmap)
        }
    }

    //Method for opening user gallery
    private fun pickImageFromGallery() {
        //permissions()
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
                binding.imageView.setImageURI(file_uri)
            }
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {
        if (fileUri != null) {
            val fileName = "$offerId.png"

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
                            StyleableToast.makeText(this, "Picture Uploaded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
                            file.delete()
                            switchBack()
                        }
                    })
                .addOnFailureListener(OnFailureListener { e ->
                    StyleableToast.makeText(this, "Issue with Uploading!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
                    file.delete()
                })
        }
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
            switchBack()
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