package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.NoUserAccBinding
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast

class NoAccount : AppCompatActivity() {
    private lateinit var binding : NoUserAccBinding
    private var data : FirebaseData = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var boolUser : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NoUserAccBinding.inflate(layoutInflater)
        checkUser()


        binding.logoutButton.setOnClickListener {
            if (boolUser){
                auth.signOut()
                StyleableToast.makeText(applicationContext, "Logged out!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            else{
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }


        setContentView(binding.root)
    }





    private fun checkDelete(){
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    if (user!!.name != "DELETED USER" && user.ballance != -1){
                        binding.logoutButton.text = "Return"
                        boolUser = false
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

        if (user != null){
            checkDelete()
        }
    }

    override fun onStart() {
        super.onStart()
        checkUser()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }



}