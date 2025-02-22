package app.code.xfp.admin

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.Login
import app.code.xfp.NoConnection
import app.code.xfp.R
import app.code.xfp.adapters.ConfirmAdapter
import app.code.xfp.adapters.UserAdapter
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.AdminPanelBinding
import app.code.xfp.databinding.NotificationBinding
import app.code.xfp.databinding.RecyclerChatBinding
import app.code.xfp.objects.Offer
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast
import java.util.*

class AdminPanel : AppCompatActivity(){
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private lateinit var binding: AdminPanelBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = AdminPanelBinding.inflate(layoutInflater)


        binding.manageOffers.setOnClickListener {
            val intent = Intent(this, AdminOffers::class.java)
            startActivity(intent)
        }

        binding.manageOrders.setOnClickListener {
            val intent = Intent(this, AdminOrders::class.java)
            startActivity(intent)
        }

        binding.manageUsers.setOnClickListener {
            val intent = Intent(this, AdminUsers::class.java)
            startActivity(intent)
        }

        binding.reportsButton.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            startActivity(intent)
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