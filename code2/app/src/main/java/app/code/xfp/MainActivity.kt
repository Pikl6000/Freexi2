package app.code.xfp

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import app.code.xfp.databinding.ActivityMainBinding
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File

class MainActivity : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding : ActivityMainBinding
    private var data : FirebaseData = FirebaseData()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        //Check if user is logged in
        checkUser()

        setContentView(binding.root)
        replaceFragment(Home())

        //Method for changing fragments
        binding.bottomNavigationView.setOnItemSelectedListener {
            val fragmentInstance = supportFragmentManager.findFragmentById(R.id.frame_layout)
            when(it.itemId){
                R.id.home_nav ->{
                    if (fragmentInstance !is Home){
                        replaceFragment(Home())
                    }
                }
                R.id.work_nav -> {
                    if (fragmentInstance !is Work){
                        replaceFragment(Work())
                    }
                }
                R.id.account_nav -> {
                    if (fragmentInstance !is Account){
                        replaceFragment(Account())
                    }
                }
            }
            true
        }
    }


    // Function for changing displayed fragments
    private fun replaceFragment(fragment : Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }
    
    private fun permissions(){
        val REQUEST_CODE = 123
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
            )
        }

        if (
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
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
            val intent = Intent(this, NotificationService::class.java)
            this.startService(intent)
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
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