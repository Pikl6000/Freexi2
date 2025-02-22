package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.ContactSupportBinding
import app.code.xfp.objects.Report
import com.google.firebase.auth.FirebaseAuth
import io.github.muddz.styleabletoast.StyleableToast
private var dialog : Dialog? = null

class Support : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private lateinit var binding: ContactSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUser()

        binding = ContactSupportBinding.inflate(layoutInflater)

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_wait)
        dialog!!.setCanceledOnTouchOutside(false)

        binding.sentButton.setOnClickListener {
            binding.sentButton.isEnabled = false
            binding.checkBox.isEnabled = false
            dialog?.show()
            var text = binding.textInputEditTextF4.text.toString()

            if (text.length < 12){
                StyleableToast.makeText(this, "Message must be at least 12 characters!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                dialog?.dismiss()
                binding.sentButton.isEnabled = true
                binding.checkBox.isEnabled = true
            }
            else if (!binding.checkBox.isChecked){
                StyleableToast.makeText(this, "You must accept our policy!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                dialog?.dismiss()
                binding.sentButton.isEnabled = true
                binding.checkBox.isEnabled = true
            }
            else{
                val report : Report = Report("Support",System.currentTimeMillis().toString(),text,auth.currentUser!!.uid,false)
                data.addReport(report).addOnCompleteListener {
                    dialog?.dismiss()
                    binding.sentButton.isEnabled = true
                    binding.checkBox.isEnabled = true
                    if (it.isSuccessful){
                        StyleableToast.makeText(this, "Message send!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()

                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    else{
                        StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                    }
                }
            }
        }

        setContentView(binding.root)
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