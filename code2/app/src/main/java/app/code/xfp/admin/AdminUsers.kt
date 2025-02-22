package app.code.xfp.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.*
import app.code.xfp.adapters.UserAdapter
import app.code.xfp.adapters.WorkAdapter
import app.code.xfp.databinding.AdminManageUserBinding
import app.code.xfp.databinding.AdminPanelBinding
import app.code.xfp.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class AdminUsers : AppCompatActivity() {
    private lateinit var binding : AdminManageUserBinding
    private var data : FirebaseData = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var userList = ArrayList<User>()
    private var orderList = ArrayList<Order>()
    private lateinit var adapter : UserAdapter
    private var selected : Int = 0
    private var email : String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminManageUserBinding.inflate(layoutInflater)
        checkUser()

        val bundle : Bundle?= intent.extras
        val possId = bundle?.getString("id").toString()

        if (possId != "null"){
            changeScene(possId)
        }

        adapter = UserAdapter(userList)
        adapter.setOnItemClickListener(object : UserAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                selected = position
                changeScene()
            }
        })


        binding.recyclerResultUser.adapter = adapter
        binding.recyclerResultUser.layoutManager = LinearLayoutManager(this)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchUsers(query.trim())
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchUsers(newText.trim())
                return false
            }
        })

        binding.con.setOnClickListener {
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("sellerId",userList.get(selected).id)
            startActivity(intent)
        }
        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("sellerId",userList.get(selected).id)
            startActivity(intent)
        }

        binding.editInformation.setOnClickListener {
            val intent = Intent(applicationContext, AccountInformation::class.java)
            if (possId != "null"){
                intent.putExtra("userId",possId)
            }
            else{
                intent.putExtra("userId",userList.get(selected).id)
            }

            startActivity(intent)
        }

        binding.editPicture.setOnClickListener {
            val intent = Intent(applicationContext, AccountPicture::class.java)
            if (possId != "null"){
                intent.putExtra("userId",possId)
            }
            else{
                intent.putExtra("userId",userList.get(selected).id)
            }
            startActivity(intent)
        }

        binding.editPassword.setOnClickListener {
            var searchId = ""
            if (possId != "null"){
                searchId = possId
            }
            else{
                searchId = userList.get(selected).id
            }

            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        email = user!!.email
                        data.getDatabaseReference()!!.removeEventListener(this)
                        sentEmail()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)
        }

        binding.editUser.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm action")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to delete this user account? This action cannot be undone !")

            builder.setPositiveButton("Delete") { dialog, which ->
                if (possId != "null"){
                    delete(possId)
                }
                else delete(userList.get(selected).id)
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        setContentView(binding.root)
    }


    private fun delete(id : String){
        val userId = userList.get(selected).id

        val hashMap = HashMap<String, Any>()
        hashMap.put("ballance",-1)
        hashMap.put("country","")
        hashMap.put("countryCode","")
        hashMap.put("date","")
        hashMap.put("description","")
        hashMap.put("joinDate","")
        hashMap.put("name","DELETED USER")
        hashMap.put("phoneNumber","")
        hashMap.put("rating","")

        //Delete orders & refund
        data.getDatabaseReferenceOrder()!!.orderByChild("status").equalTo("awaiting").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        val order = i.getValue(Order::class.java)
                        if (order!!.customerID == userId || order.sellerID == userId){
                            orderList.add(order)
                        }
                    }
                    refund(userId)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        //Delete notifications
        data.getDatabaseReferenceNotification()!!.orderByChild("toId").equalTo(userId).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        i.ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        //Delete offers
        data.getDatabaseReferenceOffer()!!.orderByChild("sellerId").equalTo(userId).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        val offer = i.getValue(Offer::class.java)
                        val refStorage = FirebaseStorage.getInstance().reference.child("offer_images/${offer!!.id}.png")
                        refStorage.delete()
                        i.ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        val refStorage = FirebaseStorage.getInstance().reference.child("profile_images/${userId}.png")
        refStorage.delete()
        data.update(userId,hashMap)
    }

    private fun refund(userId : String){
        for (order : Order in orderList){
            if (order.customerID == userId){
                val refStorage = FirebaseStorage.getInstance().reference.child("delivery/${order.id}")
                refStorage.listAll()
                    .addOnSuccessListener { listResult ->
                        if (listResult.items.isNotEmpty()) {
                            refStorage.delete()
                        }
                    }

                val hashMap = java.util.HashMap<String, Any>()
                hashMap.put("status","canceled")
                data.updateOrder(order.id,hashMap)
            }
            else if (order.sellerID == userId){
                var userBall : Int = 0
                data.getDatabaseReference()!!.orderByChild("id").equalTo(order.customerID).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (i in snapshot.children) {
                                val user = i.getValue(User::class.java)
                                if (user?.id == order.customerID && user.ballance != -1){
                                    userBall = user.ballance + order.price.toInt()
                                    sendRefund(order.customerID,userBall,order.sellerID,order.price,order.offerId)
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                val hashMap = java.util.HashMap<String, Any>()
                hashMap.put("status","canceled")
                data.updateOrder(order.id,hashMap)

                val refStorage = FirebaseStorage.getInstance().reference.child("delivery/${order.id}")
                refStorage.listAll()
                    .addOnSuccessListener { listResult ->
                        if (listResult.items.isNotEmpty()) {
                            refStorage.delete()
                        }
                    }
            }
        }
    }

    private fun sendRefund(userId: String, userBall : Int, sellerID : String, price : String, offerId : String){
        val hashMap = java.util.HashMap<String, Any>()
        hashMap.put("ballance",userBall)
        data.update(userId,hashMap).addOnCompleteListener {
            if (it.isSuccessful){
                var transaction = TransactionClass(sellerID,userId,price,offerId,System.currentTimeMillis().toString(), UUID.randomUUID().toString())
                data.addTransaction(transaction)
                val notify = NotificationObj(sellerID,userId,"notification","Refund","User that you ordered from had his account deleted. You have received refund !")
                data.addNotification(notify)
            }
        }
    }


    private fun changeScene(){
        binding.searchUser.visibility = View.GONE
        binding.resultUser.visibility = View.VISIBLE

        val user = userList.get(selected)
        binding.seller.text = user.name
        binding.fromT.text = user.country
        binding.userDescription.text = user.description

        if (user.joinDate != ""){
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = user.joinDate.toLong()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val time = "$day.$month.$year"
            binding.joinedT.text = time
        }

        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${user.id}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.profileIcon.loadImage(bitmap)
        }.addOnFailureListener{
            binding.profileIcon.loadImage(R.drawable.person)
        }
    }

    private fun changeScene(searchId : String){
        binding.searchUser.visibility = View.GONE
        binding.resultUser.visibility = View.VISIBLE

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)

                    binding.seller.text = user!!.name
                    binding.fromT.text = user.country
                    binding.userDescription.text = user.description

                    if (user.joinDate != ""){
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = user.joinDate.toLong()
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val year = calendar.get(Calendar.YEAR)
                        val time = "$day.$month.$year"
                        binding.joinedT.text = time
                    }


                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)

        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${searchId}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.profileIcon.loadImage(bitmap)
        }.addOnFailureListener{
            binding.profileIcon.loadImage(R.drawable.person)
        }
    }

    private fun sentEmail(){
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    StyleableToast.makeText(this, "Email sent!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
                    val notify = NotificationObj(auth.currentUser!!.uid,userList.get(selected).id,"warning","Password Recovery","Password recovery E-mail has been sent!")
                    data.addNotification(notify)
                }
                else{
                    StyleableToast.makeText(this, "Error, try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                }
            }

    }

    private fun searchUsers(query: String) {
        val searchQuery = data.getDatabaseReference()!!.orderByChild("name").startAt(query).endAt(query + "\uf8ff")

        searchQuery.addValueEventListener(object : ValueEventListener {
            var counter : Int = 0
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (userList.isNotEmpty()) userList.clear()
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null && user.name != "DELETED USER" && counter < 5) {
                        userList.add(user)
                        counter++
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    override fun onBackPressed() {
        if (binding.resultUser.visibility == View.VISIBLE){
            binding.resultUser.visibility = View.GONE
            binding.searchUser.visibility = View.VISIBLE
        }
        else{
            super.onBackPressed()
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
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }
}