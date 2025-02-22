package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.Window
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.LoginscreenBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.github.muddz.styleabletoast.StyleableToast

class Login : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding:LoginscreenBinding
    private var dialog : Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginscreenBinding.inflate(layoutInflater)
        checkUser()

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_loading)
        dialog!!.setCanceledOnTouchOutside(false)

        // Try to log in
        binding.loginbuttonlog.setOnClickListener{
            val email = binding.textInputEditText1.text.toString()
            val pass = binding.textInputEditText2.text.toString()

            if (isValidEmail(email) && pass.isNotEmpty()) {
                dialog?.show()
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        dialog?.dismiss()
                        StyleableToast.makeText(this, "Logged in!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        dialog?.dismiss()
                        StyleableToast.makeText(this, "Error, Please check your information!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                    }
                }
            } else {
                StyleableToast.makeText(this, "Empty Fields Are not Allowed!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
        }

        // Switch to register activity
        binding.registerbuttonlog.setOnClickListener{
            startActivity(Intent(this@Login, Register::class.java))
        }

        binding.resetText.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Reset password")
            builder.setCancelable(true)

            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.alert_dialog_reset, null)
            val input = dialogView.findViewById<TextInputEditText>(R.id.textInputEditTextR11)

            builder.setView(dialogView)

            builder.setPositiveButton("Reset") { dialog, which ->
                val email = input.text.toString()
                if (isValidEmail(email)){
                    checkEmail(email)
                }
                else{
                    StyleableToast.makeText(this, "Enter valid email!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                }
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        setContentView(binding.root)
    }

    //Function to check if user is logged in
    private fun isLoggedIn():Boolean{
        return auth.currentUser != null
    }

    //Function to check if inputted string is in right email format
    private fun isValidEmail(target: String): Boolean {
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }

    private fun checkEmail(email : String){
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        StyleableToast.makeText(this, "Error, this email is not registered!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                    } else {
                        sendPasswordResetEmail(email)
                    }
                } else {
                    StyleableToast.makeText(this, "Error, Please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    StyleableToast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                } else {
                    StyleableToast.makeText(this, "Error sending password reset email!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
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

