package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.code.xfp.databinding.AcceptOrderBinding
import app.code.xfp.databinding.ChatRoomBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.Order
import app.code.xfp.objects.TransactionClass
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AcceptOrder : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding : AcceptOrderBinding
    private var orderId : String = ""
    private var userBall : Int = 0
    private var orderPrice : Int = 0
    private var customerId : String = ""
    private var title : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = AcceptOrderBinding.inflate(layoutInflater)

        orderId = intent.getStringExtra("orderId").toString()

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val order = snapshot.getValue(Order::class.java)

                    binding.delivery.text = order!!.deliveryDate
                    binding.orderTitle.text = order.offerTitle
                    val desc = Html.fromHtml(order.description)
                    binding.requirement.text = desc
                    orderPrice = order.price.toInt()
                    customerId = order.customerID
                    title = order.offerTitle

                    val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${order.customerID}.png")
                    var localfile = File.createTempFile("tempImage2","png")
                    storageReference.getFile(localfile).addOnSuccessListener {
                        Glide.with(applicationContext)
                            .load(localfile.absolutePath)
                            .apply(RequestOptions().centerCrop())
                            .apply(RequestOptions().override(50, 50))
                            .into(binding.profileIcon)
                    }.addOnFailureListener{
                        Glide.with(applicationContext)
                            .load(R.drawable.person)
                            .apply(RequestOptions().centerCrop())
                            .apply(RequestOptions().override(50, 50))
                            .into(binding.profileIcon)
                    }

                    if (order.orderDate != ""){
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = order.orderDate.toLong()
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val year = calendar.get(Calendar.YEAR)
                        val time = "$day.$month.$year"
                        binding.deliveryDate.text = time
                    }

                    //Getting information from database and putting showing it
                    val listener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (dataP in dataSnapshot.children) {
                                val user = dataP.getValue(User::class.java)
                                binding.username.text = user!!.name
                                userBall = user.ballance
                                data.getDatabaseReference()!!.removeEventListener(this)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            data.getDatabaseReference()!!.removeEventListener(this)
                        }
                    }
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.customerID).addListenerForSingleValueEvent(listener)
                    data.getDatabaseReferenceOrder()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                data.getDatabaseReferenceOrder()!!.removeEventListener(this)
            }
        }
        data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(orderId).addListenerForSingleValueEvent(listener)


        binding.customerInfoC.setOnClickListener{
            val intent = Intent(applicationContext,UserProfile::class.java)
            intent.putExtra("sellerId",customerId)
            startActivity(intent)
        }

        binding.declineButton.setOnClickListener{
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm selected")
            builder.setMessage("Are you sure you want to decline this order ?")

            builder.setPositiveButton("Decline") { dialog, which ->
                declineOrder()
            }

            builder.setNegativeButton("Return") { dialog, which ->
            }
            builder.show()
        }

        binding.acceptButton.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm selected")
            builder.setMessage("Are you sure you want to accept this order ?")

            builder.setPositiveButton("Accept") { dialog, which ->
                acceptOrder()
            }

            builder.setNegativeButton("Return") { dialog, which ->
            }
            builder.show()
        }


        setContentView(binding.root)
    }

    private fun acceptOrder(){
        val hashMap = HashMap<String, Any>()
        hashMap.put("accepted",true)
        hashMap.put("status","awaiting")

        data.updateOrder(orderId,hashMap)
        data.updateOrder(orderId,hashMap).addOnSuccessListener {
            val notify = NotificationObj(auth.currentUser!!.uid,customerId,"order","Order Accepted","Your order of $title has just been accepted!")
            data.addNotification(notify)
            StyleableToast.makeText(this, "Order Accepted!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun declineOrder(){
        val hashMap = HashMap<String, Any>()
        hashMap.put("accepted",true)
        hashMap.put("status","declined")

        data.updateOrder(orderId,hashMap).addOnSuccessListener {
            refund(orderPrice)
            val notify = NotificationObj(auth.currentUser!!.uid,customerId,"warning","Order Declined","Your order of $title has been declined!")
            data.addNotification(notify)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun refund(amount : Int){
        var ball  = userBall + amount

        val hashMap = HashMap<String, Any>()
        hashMap.put("ballance",ball)

        data.update(customerId,hashMap).addOnSuccessListener {
            var transaction = TransactionClass(auth.currentUser!!.uid,customerId,orderPrice.toString(),orderId,getCurrentDate(), UUID.randomUUID().toString(),true)
            data.addTransaction(transaction)
            StyleableToast.makeText(this, "Declined & Refunded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
        }
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