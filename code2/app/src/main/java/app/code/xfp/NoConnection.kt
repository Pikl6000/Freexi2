package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.NoConnectionBinding
import io.github.muddz.styleabletoast.StyleableToast

class NoConnection : AppCompatActivity(){
    private lateinit var binding: NoConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NoConnectionBinding.inflate(layoutInflater)
        checkInternet()
        binding.retryButton.setOnClickListener{
            checkInternet()
        }
        setContentView(binding.root)
    }

    private fun checkInternet() {
        if (isNetworkAvailable()) {
            StyleableToast.makeText(this, "Reconnected!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            StyleableToast.makeText(this, "No Connection", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}