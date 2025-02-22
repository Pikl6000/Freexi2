package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.SendOrderBinding
import app.code.xfp.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class SendOrder : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: SendOrderBinding
    private var userId : String = ""
    private var sellerId : String = ""
    private var offerId : String = ""
    private var userBall : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()
        data = FirebaseData()

        binding = SendOrderBinding.inflate(layoutInflater)

        //Getting data from Bundle and Setting them
        val bundle : Bundle?= intent.extras

        binding.titleCard.text = bundle!!.getString("title")
        binding.username.text = bundle.getString("seller")
        binding.price.text = bundle.getString("price")
        sellerId = bundle.getString("sellerId").toString()
        offerId = bundle.getString("id").toString()


        //Getting profile image from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)

                    if (user!!.id.equals(sellerId)){
                        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${user.id}.png")
                        var localfile = File.createTempFile("tempImage","png")
                        storageReference.getFile(localfile).addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            binding.profileIcon.loadImage(bitmap)
                        }.addOnFailureListener{
                            binding.profileIcon.loadImage(R.drawable.person)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(sellerId).addListenerForSingleValueEvent(listener)

        //Getting user ID
        val listener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    userId = user!!.id
                    userBall = user.ballance
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener2)

        //Alert dialog for confirmation of creating order
        binding.orderButton.setOnClickListener{
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm order")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to order this offer ?")

            builder.setPositiveButton("Order") { dialog, which ->
                createOrder()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        setContentView(binding.root)
    }

    private fun createOrder(){
        var description : String = binding.textInputEditTextO5.text.toString()
        description = description.replace("\n", "<br>")

        if (description.isEmpty()){
            StyleableToast.makeText(this, "You must enter description for seller", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
        else if (description.length < 12){
            StyleableToast.makeText(this, "Description must be at least 12 characters", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
        else{
            if (userBall > binding.price.text.toString().toInt()){
                var transaction = TransactionClass(auth.currentUser!!.uid,sellerId,binding.price.text.toString(),offerId,getCurrentDate(),UUID.randomUUID().toString())
                data.addTransaction(transaction)
                var order = Order(UUID.randomUUID().toString(),sellerId,userId,getCurrentDate(),binding.price.text.toString(),description,offerId,binding.titleCard.text.toString(),transaction.id)
                data.addOrder(order).addOnCompleteListener {
                    if (it.isSuccessful) {
                        charge(order.price.toInt())
                        createNotification(auth.currentUser?.uid.toString(),order.id)
                        StyleableToast.makeText(this, "Order send, await respond from seller", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        StyleableToast.makeText(this, "Error, please try later", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                    }
                }
            }
            else {
                StyleableToast.makeText(this, "Error, insufficient funds!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        }
    }

    private fun createNotification(userId : String,nId : String){
        val notify = NotificationObj(userId,sellerId,"notification","Order","You have received an order")
        data.addNotification(notify)
        Log.v("Notify","Created notification")
    }


    private fun charge(amount : Int){
        userBall -= amount
        val hashMap = HashMap<String, Any>()
        hashMap.put("ballance",userBall)

        data.update(auth.currentUser!!.uid,hashMap)
    }

    private fun checkBall(price : Int):Boolean{
        data.getDatabaseReference()!!.orderByChild("id").equalTo(auth.currentUser?.uid).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        val user = i.getValue(User::class.java)
                        userBall = user!!.ballance
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        return userBall > price
    }


    private fun getCurrentDate(): String{
        return System.currentTimeMillis().toString()
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