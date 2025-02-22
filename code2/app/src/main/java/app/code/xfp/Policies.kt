package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.OfferManageBinding
import app.code.xfp.databinding.PoliciesBinding
import io.github.muddz.styleabletoast.StyleableToast

class Policies : AppCompatActivity() {
    private lateinit var binding: PoliciesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PoliciesBinding.inflate(layoutInflater)
        checkInternet()
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