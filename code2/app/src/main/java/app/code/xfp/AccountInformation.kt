package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.AccountInformationBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast

class AccountInformation : AppCompatActivity() {
    private lateinit var binding : AccountInformationBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private var userId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        binding = AccountInformationBinding.inflate(layoutInflater)


        val bundle : Bundle?= intent.extras
        userId = bundle?.getString("userId")

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        data = FirebaseData()

        binding.ccp.registerCarrierNumberEditText(binding.textInputd2);

        if (userId.isNullOrEmpty()){
            userId = auth.currentUser!!.uid
        }

        if (userId != auth.currentUser!!.uid){
            binding.acc2.visibility = View.GONE
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    val name = user!!.name
                    val phone = user.phoneNumber.toString()
                    val code = user.countryCode
                    val description = user.description

                    binding.ccp.setCountryForPhoneCode(Integer.parseInt(code))

                    val desc = Html.fromHtml(description)
                    binding.textInputd1.setText(name)
                    binding.textInputd2.setText(phone.substringAfter(code))
                    binding.textInputEditTextA3.setText(desc)

                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(userId).addListenerForSingleValueEvent(listener)

        binding.savebutton.setOnClickListener{
            val inName = binding.textInputd1.text.toString()
            var inPhone : String = binding.ccp.formattedFullNumber
            val country = binding.ccp.selectedCountryCode
            val countryName = binding.ccp.selectedCountryName
            var description = binding.textInputEditTextA3.text.toString()
            description = description.replace("\n", "<br>")

            val hashMap = HashMap<String, Any>()
            hashMap.put("description",description)
            hashMap.put("countryCode",country)
            hashMap.put("name",inName)
            hashMap.put("country",countryName)
            if (binding.ccp.isValidFullNumber) hashMap.put("phoneNumber", inPhone.substringAfter(country))
            data.update(userId!!,hashMap).addOnCompleteListener {
                if (it.isSuccessful) {
                    if (userId != auth.currentUser!!.uid){
                        val notify = NotificationObj(auth.currentUser!!.uid,userId!!,"warning","Account Edit","Your account has been edited by administrator !")
                        data.addNotification(notify)
                    }
                    StyleableToast.makeText(this, "Saved!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                else StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
            }
        }

        setContentView(binding.root)
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

        if (user == null){ //If he's not (switch to login activity)
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        else{
            checkDelete()
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