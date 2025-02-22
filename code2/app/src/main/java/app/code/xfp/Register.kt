package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.Register2Binding
import app.code.xfp.databinding.Register3Binding
import app.code.xfp.databinding.RegisterBinding
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


public class Register : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding: RegisterBinding
    private lateinit var binding2: Register2Binding
    private lateinit var binding3: Register3Binding
    private var data: FirebaseData = FirebaseData()
    private lateinit var storage: FirebaseStorage
    var found = false
    var img = false
    private var imgpath : Uri = Uri.EMPTY
    internal var user: User? = null
    private var dialog : Dialog? = null

    companion object {
        val IMAGE_REQUEST_CODE = 1_000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()

        binding = RegisterBinding.inflate(layoutInflater)
        binding2 = Register2Binding.inflate(layoutInflater)
        binding3 = Register3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_loading)
        dialog!!.setCanceledOnTouchOutside(false)


        binding.nextbutton.setOnClickListener{
            setContentView(binding2.root)
            binding2.textInputEditText5.setEnabled(false)
            binding2.ccp.registerCarrierNumberEditText(binding2.textInputEditText6);
        }

        binding.backlogbutt.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding2.returnB.setOnClickListener{
            setContentView(binding.root)
        }

        binding2.selectday.setOnClickListener {
            val minDate = Calendar.getInstance().apply {
                set(1930, Calendar.JANUARY, 1)
            }.timeInMillis
            val maxDate = Calendar.getInstance().timeInMillis
            val calendarConstraints = CalendarConstraints.Builder()
                .setStart(minDate)
                .setEnd(maxDate)
                .build()


            val builder = MaterialDatePicker.Builder.datePicker()
            builder.setCalendarConstraints(calendarConstraints)
            builder.setSelection(maxDate)
            val picker = builder.build()
            picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
            picker.addOnPositiveButtonClickListener {
                val selectedDate = it
                if (!isDateAfterToday(selectedDate)){
                    println(selectedDate)
                    binding2.textInputEditText5.setText(picker.headerText)
                }
                else {
                    StyleableToast.makeText(this, "Enter valid date!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
            }
        }

        binding2.checkBoxText.setOnClickListener{
            val intent = Intent(this, Policies::class.java)
            startActivity(intent)
        }

        binding2.nextbutton.setOnClickListener{
            val email = binding.textInputEditTextR11.text.toString()
            val pass = binding.textInputEditTextR12.text.toString()
            val name = binding.textInputEditTextR13.text.toString()
            val phone = binding2.ccp.getFullNumberWithPlus()
            val date = binding2.textInputEditText5.text.toString()
            val countryCode = binding2.ccp.selectedCountryCode
            val country = binding2.ccp.selectedCountryName


            if (!isValidEmail(email)){
                StyleableToast.makeText(this, "Enter valid E-mail!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if (isValidPass(pass)){
                StyleableToast.makeText(this, "Enter valid Password!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if (!PhoneNumberUtils.isGlobalPhoneNumber(phone)){
                StyleableToast.makeText(this, "Enter valid Phone number!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if (date.isEmpty()){
                StyleableToast.makeText(this, "Enter valid date!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if(isValidNick(name)){
                StyleableToast.makeText(this, "Name must be at least 4 characters!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if (name == "DELETED USER"){
                StyleableToast.makeText(this, "Cannot use this name!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else if (!binding2.checkBox.isChecked){
                StyleableToast.makeText(this, "You have to accept our policy!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else{
                dialog?.show()
                val listener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        found = true
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }
                data.getDatabaseReference()!!.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(listener)

                if (!found){
                    val user1 = User(name,email,phone,250,countryCode,country,date,"",getCurrentDate())
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            dialog?.dismiss()
                            StyleableToast.makeText(this, "Registered!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                            createUser(user1,auth.currentUser!!.uid)
                            user = user1
                            setContentView(binding3.root)
                        } else {
                            dialog?.dismiss()
                            StyleableToast.makeText(this, "Error, please try again!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                        }
                    }
                }
                else{
                    dialog?.dismiss()
                    StyleableToast.makeText(this, "Already registered!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
            }
        }

        binding3.returnB2.setOnClickListener{
            setContentView(binding2.root)
        }

        binding3.registerbutton.setOnClickListener{
            if (!img){
                StyleableToast.makeText(this, "Please select your profile picture!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
            else{
                uploadImageToFirebase(imgpath)
            }
        }
        binding3.uploadpicture.setOnClickListener{
            pickImageFromGallery()
        }

        binding.textInputEditTextR12.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString().trim()

                if (password.length >= 8) binding.textView45.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0)
                else binding.textView45.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)

                if (password.filter { it.isDigit() }.firstOrNull() != null) binding.textView43.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0)
                else binding.textView43.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)

                if (password.filter { it.isLetter() }.filter { it.isLowerCase() }.firstOrNull() != null && password.filter { it.isLetter() }.filter { it.isUpperCase() }.firstOrNull() != null)
                    binding.textView44.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0)
                else binding.textView44.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)
            }
        })
        
    }

    fun isDateAfterToday(selectedDate: Long): Boolean {
        val today = Calendar.getInstance()
        val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        return selectedCalendar.after(today)
    }

    private fun createUser(p: User, autoID : String){
        p.id = autoID
        data.add(p)
    }

    private fun getCurrentDate(): String{
        return System.currentTimeMillis().toString()
    }

    //Function to check if inputted string is in right email format
    private fun isValidEmail(target: String): Boolean {
        println(target)
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }
    //Function to check if inputted string is in right format
    private fun isValidNick(target: String): Boolean {
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            return (target.length <= 4) ||
                    (target.length >= 28)
        }
    }
    //Function to check if inputted string is in right password format
    private fun isValidPass(password: String): Boolean {
        if (password.length < 8) return false
        if (password.filter { it.isDigit() }.firstOrNull() == null) return false
        if (password.filter { it.isLetter() }.filter { it.isUpperCase() }.firstOrNull() == null) return false
        if (password.filter { it.isLetter() }.filter { it.isLowerCase() }.firstOrNull() == null) return false
        if (password.filter { !it.isLetterOrDigit() }.firstOrNull() == null) return false

        return true
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            val file_uri = data.data
            if (file_uri != null) {
                img = true
                imgpath = file_uri
                Glide.with(this)
                    .load(file_uri)
                    .into(binding3.profileR)
            }
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {
        checkInternet()
        if (fileUri != null) {
            binding3.registerbutton.isEnabled = false
            binding3.uploadpicture.isEnabled = false
            dialog?.show()
            val fileName = user!!.id +".png"

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
                            dialog?.dismiss()
                            binding3.registerbutton.isEnabled = true
                            binding3.uploadpicture.isEnabled = true
                            file.delete()
                            StyleableToast.makeText(this, "Picture Uploaded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show();
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    })

                .addOnFailureListener(OnFailureListener { e ->
                    StyleableToast.makeText(this, "Error, please try again!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                    dialog?.dismiss()
                    file.delete()
                    binding3.registerbutton.isEnabled = true
                    binding3.uploadpicture.isEnabled = true
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

        if (user != null){ //If he's not (switch to login activity)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    override fun onStart() {
        super.onStart()
        checkInternet()
    }

    override fun onResume() {
        super.onResume()
        checkInternet()
    }

    override fun onRestart() {
        super.onRestart()
        checkInternet()
    }
}


