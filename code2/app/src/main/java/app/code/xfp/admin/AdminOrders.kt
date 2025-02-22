package app.code.xfp.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.Login
import app.code.xfp.ManageOffer
import app.code.xfp.NoConnection
import app.code.xfp.R
import app.code.xfp.UserProfile
import app.code.xfp.adapters.OrderAdapter
import app.code.xfp.databinding.AdminManageOrderBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.Offer
import app.code.xfp.objects.Order
import app.code.xfp.objects.TransactionClass
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.util.ArrayList
import java.util.UUID

class AdminOrders : AppCompatActivity() {
    private var data : FirebaseData = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding : AdminManageOrderBinding

    private var selected : Int = 0
    private var selectedId : String = ""
    private var list = ArrayList<Order>()
    private var showList = ArrayList<Order>()
    private var adapter = OrderAdapter(list)
    private var maxSearch : Int = 3
    private var possId : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminManageOrderBinding.inflate(layoutInflater)
        checkUser()

        val bundle : Bundle?= intent.extras
        possId = bundle?.getString("id").toString()

        if (possId != "null"){
            selectedId = possId
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val order = dataP.getValue(Order::class.java)
                        list.add(order!!)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(selectedId).addListenerForSingleValueEvent(listener)
            
            selected = 0
            changeScene(possId)
        }

        adapter.setOnItemClickListener(object : OrderAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                selected = position
                selectedId = list.get(position).id
                changeScene()
            }
        })

        binding.recyclerResultOrder.adapter = adapter
        binding.recyclerResultOrder.layoutManager = LinearLayoutManager(this)

        binding.editInformation.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm action")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to decline this order?")
            builder.setPositiveButton("Decline") { dialog, which ->
                declineOrder()
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
            }
            builder.show()
        }

        binding.deleteOrder.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm action")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to delete this order?")
            builder.setPositiveButton("Delete") { dialog, which ->
                deleteOrder()
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
            }
            builder.show()
        }


        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchOrders(query.trim(),maxSearch)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchOrders(newText.trim(),maxSearch)
                return false
            }
        })

        binding.username.setOnClickListener {
            val intent = Intent(applicationContext,UserProfile::class.java)
            intent.putExtra("sellerId",list.get(selected).customerID)
            startActivity(intent)
        }

        binding.usernameSeller.setOnClickListener {
            val intent = Intent(applicationContext, UserProfile::class.java)
            intent.putExtra("sellerId",list.get(selected).sellerID)
            startActivity(intent)
        }

        binding.recyclerResultOrder.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Check if the user has reached the end of the list
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val isAtEnd = visibleItemCount + firstVisibleItemPosition >= totalItemCount

                if (isAtEnd) {
                    loadNextChunkOfData()
                }
            }
        })

        setContentView(binding.root)
    }

    private fun deleteOrder(){
        if (list.get(selected).status != "declined" || list.get(selected).status != "delivered"){
            data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(selectedId).addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snapshot1 in snapshot.children){
                        snapshot1.ref.removeValue()
                        val notificationCustomer = NotificationObj(auth.currentUser!!.uid,list.get(selected).customerID,"warning","Order deleted","Your order of ${list.get(selected).offerTitle} has been deleted")
                        val notificationSeller = NotificationObj(auth.currentUser!!.uid,list.get(selected).sellerID,"warning","Order deleted","Your order of ${list.get(selected).offerTitle} has been deleted")

                        val refund = TransactionClass(list.get(selected).sellerID,list.get(selected).customerID,list.get(selected).price,selectedId,System.currentTimeMillis().toString(),
                            UUID.randomUUID().toString(),true)

                        data.addTransaction(refund)
                        data.addNotification(notificationCustomer)
                        data.addNotification(notificationSeller)

                        StyleableToast.makeText(applicationContext, "Order Deleted!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
            })
        }
        else {
            StyleableToast.makeText(applicationContext, "Error, order already delivered!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun declineOrder(){
        if (list.get(selected).status != "declined" || list.get(selected).status != "delivered"){
            data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(selectedId).addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snapshot1 in snapshot.children){
                        val hashMap = HashMap<String, Any>()
                        hashMap.put("accepted",true)
                        hashMap.put("status","declined")

                        data.updateOrder(selectedId,hashMap).addOnCompleteListener {
                            if (it.isSuccessful){

                                val notificationCustomer = NotificationObj(auth.currentUser!!.uid,list.get(selected).customerID,"warning","Order declined","Your order of ${list.get(selected).offerTitle} has been declined by administrator!")
                                val notificationSeller = NotificationObj(auth.currentUser!!.uid,list.get(selected).sellerID,"warning","Order declined","Your order of ${list.get(selected).offerTitle} has been declined by administrator!")

                                val refund = TransactionClass(list.get(selected).sellerID,list.get(selected).customerID,list.get(selected).price,selectedId,System.currentTimeMillis().toString(),
                                    UUID.randomUUID().toString(),true)

                                data.addTransaction(refund)
                                data.addNotification(notificationCustomer)
                                data.addNotification(notificationSeller)
                                StyleableToast.makeText(applicationContext, "Order Declined!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                            }
                            else{
                                StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
            })
        }
        else{
            StyleableToast.makeText(applicationContext, "Error, order already delivered or declined!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun loadNextChunkOfData(){
        maxSearch += 3
        val newItems = ArrayList<Order>()
        var new : Boolean = false

        if (showList.size > list.size){
            showList.clear()
        }

        for (order in list) {
            if (!showList.contains(order)) {
                if (newItems.size >= maxSearch - showList.size) {
                    break
                }
                else{
                    new = true
                    newItems.add(order)
                }
            }
        }
        if (new){
            showList.addAll(newItems)
            maxSearch = showList.size
            adapter.notifyDataSetChanged()
        }
        else{
            maxSearch -= 3
        }
    }

    private fun changeScene(){
        binding.searchOrder.visibility = View.GONE
        binding.resultOrder.visibility = View.VISIBLE

        var searchId = list.get(selected).id

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val order = dataP.getValue(Order::class.java)
                    binding.titleOrder.text = order!!.offerTitle
                    binding.orderId.text = order.id
                    binding.orderStatusT.text = order.status

                    //Getting information about the customer
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.customerID).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (snapshot1 in snapshot.children){
                                val user = snapshot1.getValue(User::class.java)
                                binding.username.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                        }
                    })
                    val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${order.customerID}.png")
                    var localfile = File.createTempFile("tempImage","png")
                    storageReference.getFile(localfile).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                        Glide.with(applicationContext)
                            .load(bitmap)
                            .into(binding.profileIcon)

                    }.addOnFailureListener{
                        Glide.with(applicationContext)
                            .load(R.drawable.person)
                            .into(binding.profileIcon)
                    }

                    //Getting information about the seller
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.sellerID).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (snapshot1 in snapshot.children){
                                val user = snapshot1.getValue(User::class.java)
                                binding.usernameSeller.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                        }
                    })
                    val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${order.sellerID}.png")
                    var localfile2 = File.createTempFile("tempImage2","png")
                    storageReference2.getFile(localfile2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
                        Glide.with(applicationContext)
                            .load(bitmap)
                            .into(binding.profileIcon2)

                    }.addOnFailureListener{
                        Glide.with(applicationContext)
                            .load(R.drawable.person)
                            .into(binding.profileIcon2)
                    }

                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)
    }

    private fun changeScene(searchId : String){
        binding.searchOrder.visibility = View.GONE
        binding.resultOrder.visibility = View.VISIBLE

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val order = dataP.getValue(Order::class.java)
                    binding.titleOrder.text = order!!.offerTitle
                    binding.orderId.text = order.id
                    binding.orderStatusT.text = order.status

                    //Getting information about the customer
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.customerID).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (snapshot1 in snapshot.children){
                                val user = snapshot1.getValue(User::class.java)
                                binding.username.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                        }
                    })
                    val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${order.customerID}.png")
                    var localfile = File.createTempFile("tempImage","png")
                    storageReference.getFile(localfile).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                        Glide.with(applicationContext)
                            .load(bitmap)
                            .into(binding.profileIcon)

                    }.addOnFailureListener{
                        Glide.with(applicationContext)
                            .load(R.drawable.person)
                            .into(binding.profileIcon)
                    }

                    //Getting information about the seller
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.sellerID).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (snapshot1 in snapshot.children){
                                val user = snapshot1.getValue(User::class.java)
                                binding.usernameSeller.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                        }
                    })
                    val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${order.sellerID}.png")
                    var localfile2 = File.createTempFile("tempImage2","png")
                    storageReference2.getFile(localfile2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
                        Glide.with(applicationContext)
                            .load(bitmap)
                            .into(binding.profileIcon2)

                    }.addOnFailureListener{
                        Glide.with(applicationContext)
                            .load(R.drawable.person)
                            .into(binding.profileIcon2)
                    }

                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)
    }

    private fun searchOrders(query: String, max: Int) {
        val searchQuery = data.getDatabaseReferenceOrder()!!.orderByChild("offerTitle").startAt(query).endAt(query + "\uf8ff")

        searchQuery.addValueEventListener(object : ValueEventListener {
            var counter : Int = 0
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (list.isNotEmpty()) list.clear()
                for (snapshot in dataSnapshot.children) {
                    val order = snapshot.getValue(Order::class.java)
                    if (counter < max) {
                        list.add(order!!)
                        counter++
                    }
                }
                val comparator = Comparator<Order> { obj1, obj2 ->
                    val date1 = obj1.orderDate.toLongOrNull() ?: 0L
                    val date2 = obj2.orderDate.toLongOrNull() ?: 0L
                    date2.compareTo(date1)
                }
                list.sortWith(comparator)

                loadNextChunkOfData()
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    override fun onBackPressed() {
        if (binding.resultOrder.visibility == View.VISIBLE){
            binding.resultOrder.visibility = View.GONE
            binding.searchOrder.visibility = View.VISIBLE
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
        checkUser()
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }
}