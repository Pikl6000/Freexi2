package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.text.Html
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.AccountInformationBinding
import app.code.xfp.databinding.AccountPasswordBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File

class AccountPassword : AppCompatActivity() {
    private lateinit var binding : AccountPasswordBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUser()
        binding = AccountPasswordBinding.inflate(layoutInflater)

        val user = auth.currentUser

        setContentView(binding.root)


        binding.passchange.setOnClickListener{
            val pass1 = binding.textInputP1.text.toString()
            val pass2 = binding.textInputP2.text.toString()

            if (isValidPass(pass1) && pass1.equals(pass2)){
                user!!.updatePassword(pass1).addOnCompleteListener {task ->
                    if (task.isSuccessful) {
                        StyleableToast.makeText(this, "Password updated!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
                        switchBack()
                    }
                    else {
                        StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                    }
                }
            }
            else{
                StyleableToast.makeText(this, "Enter valid passwords!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
        }
    }

    private fun switchBack(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun isValidPass(password: String): Boolean {
        if (password.length < 8) return false
        if (password.filter { it.isDigit() }.firstOrNull() == null) return false
        if (password.filter { it.isLetter() }.filter { it.isUpperCase() }.firstOrNull() == null) return false
        if (password.filter { it.isLetter() }.filter { it.isLowerCase() }.firstOrNull() == null) return false
        return true
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