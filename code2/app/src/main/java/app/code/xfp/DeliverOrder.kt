package app.code.xfp

import android.app.Dialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.RatingBar
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.DeliveryAdapter
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.DeliverOrderBinding
import app.code.xfp.objects.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.nio.channels.Channels
import java.text.SimpleDateFormat
import java.util.*


class DeliverOrder : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: DeliverOrderBinding
    private var orderId : String = ""
    private var files = ArrayList<DeliveryFile>()
    private var adapter = DeliveryAdapter(files)
    private var locked : Boolean = false
    var userId : String = ""
    private var offerId : String = ""
    private var userBall : Int = 0
    private var orderPrice : Int = 0
    private var transactionId : String = ""
    private var customerID : String = ""
    private var sellerID : String = ""

    private var dialog : Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = DeliverOrderBinding.inflate(layoutInflater)
        orderId = intent.getStringExtra("orderId").toString()

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_loading)
        dialog!!.setCanceledOnTouchOutside(false)

        binding.recyclerDelivery.adapter = adapter
        binding.recyclerDelivery.layoutManager =  LinearLayoutManager(this)

        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        val user = i.getValue(User::class.java)
                        userBall = user!!.ballance
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.report.setOnClickListener{
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Report Order")
            builder.setCancelable(true)

            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.alert_dialog_message, null)
            val input = dialogView.findViewById<TextInputEditText>(R.id.textInputEditTextA1)

            builder.setView(dialogView)

            builder.setPositiveButton("Report") { dialog, which ->
                checkReport(auth.currentUser?.uid.toString(),orderId,input.text.toString().trim())
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }


        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val order = dataP.getValue(Order::class.java)

                    binding.titleOrder.text = order!!.offerTitle
                    binding.deliveryDate.text = order.deliveryDate
                    val desc = Html.fromHtml(order.description)
                    binding.orderRequest.text = desc
                    userId = order.customerID
                    offerId = order.offerId
                    orderPrice = order.price.toInt()
                    transactionId = order.transactionId
                    customerID = order.customerID
                    sellerID = order.sellerID

                    if (order.sellerID == auth.currentUser!!.uid && order.status != "delivered"){
                        binding.deliveryButton.visibility = View.VISIBLE
                        binding.uploadButton.visibility = View.VISIBLE
                    }
                    else if (order.status == "delivered" && order.customerID == auth.currentUser!!.uid){
                        binding.rateButton.visibility = View.VISIBLE
                    }
                    else if (order.status == "awaiting" && order.customerID == auth.currentUser!!.uid){
                        binding.deliveryItemsC.visibility = View.GONE
                    }

                    if (!order.accepted){
                        binding.report.visibility = View.GONE
                    }

                    //Getting user name from database and putting showing it
                    val listener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (dataP in dataSnapshot.children) {
                                val user = dataP.getValue(User::class.java)
                                binding.username.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    }
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.customerID).addListenerForSingleValueEvent(listener)

                    val listener2 = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (dataP in dataSnapshot.children) {
                                val user = dataP.getValue(User::class.java)
                                binding.usernameSeller.text = user!!.name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    }
                    data.getDatabaseReference()!!.orderByChild("id").equalTo(order.sellerID).addListenerForSingleValueEvent(listener2)

                    //Downloading user profile picture
                    val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${order.customerID}.png")
                    var localfile = File.createTempFile("tempImage","png")
                    storageReference.getFile(localfile).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                        binding.profileIcon.loadImage(bitmap)
                    }.addOnFailureListener{
                        binding.profileIcon.loadImage(R.drawable.person)
                    }

                    val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${order.sellerID}.png")
                    var localfile2 = File.createTempFile("tempImage","png")
                    storageReference2.getFile(localfile2).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
                        binding.profileIcon2.loadImage(bitmap)
                    }.addOnFailureListener{
                        binding.profileIcon2.loadImage(R.drawable.person)
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

                    //Getting delivered files and downloading them
                    data.getDatabaseReferenceOrder()!!.child(order.id).child("delivery_items").addValueEventListener(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            files.clear()

                            //Getting informations
                            for (snapshot1 in snapshot.children){
                                val file : DeliveryFile = snapshot1.getValue(DeliveryFile::class.java)!!
                                files.add(file)
                            }
                            adapter.notifyDataSetChanged()

                            //On click download
                            adapter.setOnItemClickListener(object : DeliveryAdapter.onItemClickListener{
                                override fun onItemClick(position: Int) {
                                    lockAllInput()
                                    val order = files.get(position)
                                    val url = URL(order.path)
                                    val policy = ThreadPolicy.Builder().permitAll().build()
                                    StrictMode.setThreadPolicy(policy)
                                    val type = order.type
                                    val filename = "${System.currentTimeMillis()}.$type"
                                    val request = DownloadManager.Request(Uri.parse(url.toString()))
                                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                                        .setTitle(filename)
                                        .setDescription("Freexi File")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setAllowedOverMetered(true)
                                        .setAllowedOverRoaming(false)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename)
                                    val downloadManager= getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val downloadID = downloadManager.enqueue(request)
                                    StyleableToast.makeText(applicationContext, "File Saved", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
                                    lockAllInput()
                                }

                                override fun onLongClick(position: Int) {
                                    if (order.status != "delivered"){
                                        delete(position)
                                    }
                                    true
                                }
                            })

                            binding.recyclerDelivery.scrollToPosition(files.size -1)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOrder()!!.orderByChild("id").equalTo(orderId).addListenerForSingleValueEvent(listener)

        binding.rateButton.setOnClickListener{
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Rate offer")
            builder.setCancelable(true)

            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.alert_dialog, null)
            val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialog_ratingbar)

            builder.setView(dialogView)

            builder.setPositiveButton("Rate") { dialog, which ->
                val rating = ratingBar.rating.toFloat()
                rateOffer(rating)
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        binding.customerInfoC.setOnClickListener{
            val intent = Intent(applicationContext,UserProfile::class.java)
            intent.putExtra("sellerId",customerID)
            startActivity(intent)
        }

        binding.sellerInfoC.setOnClickListener{
            val intent = Intent(applicationContext,UserProfile::class.java)
            intent.putExtra("sellerId",sellerID)
            startActivity(intent)
        }


        binding.uploadButton.setOnClickListener {
            val intent = Intent(ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            startActivityForResult(intent, 1001)
        }

        binding.deliveryButton.setOnClickListener {
            if (files.isNotEmpty()){
                val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
                builder.setTitle("Confirm Delivery")
                builder.setCancelable(true)
                builder.setMessage("Are you sure you deliver uploaded files? (You won't be able to change them)")

                builder.setPositiveButton("Deliver") { dialog, which ->
                    deliver()
                }

                builder.setNegativeButton("Cancel") { dialog, which ->

                }
                builder.show()
            }
            else StyleableToast.makeText(applicationContext, "You have to upload files first!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }


        setContentView(binding.root)
    }


    private fun checkReport(userId : String,reportedId : String,message : String){
        if (message.isEmpty()){
            StyleableToast.makeText(applicationContext, "You have to enter message!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
        else{
            val id = userId+reportedId
            data.getDatabaseReferenceReport()!!.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var found = false
                    if (snapshot.exists()) {
                        for (i in snapshot.children) {
                            val report = i.getValue(Report::class.java)
                            if (report!!.reportedId == reportedId) {
                                StyleableToast.makeText(applicationContext, "You already reported this offer!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                                found = true
                            }
                        }
                        if (!found) {
                            reportOrder(userId, reportedId, message)
                            Log.v("Report","Creating report")
                            data.getDatabaseReferenceReport()!!.removeEventListener(this)
                        }
                        else Log.v("Report error","Report already created")
                        data.getDatabaseReferenceReport()!!.removeEventListener(this)
                    }
                    else{
                        reportOrder(userId, reportedId, message)
                        Log.v("Report","Creating report")
                        data.getDatabaseReferenceReport()!!.removeEventListener(this)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    data.getDatabaseReferenceReport()!!.removeEventListener(this)
                }
            })
        }
    }

    private fun reportOrder(userId: String, reportedId: String, message: String){
        var report : Report = Report("Order",System.currentTimeMillis().toString(),reportedId,message,userId,false)
        data.addReport(report).addOnCompleteListener {
            if (it.isSuccessful) {
                val notify = NotificationObj(userId,sellerID,"warning","Order Report","Your order have been reported!")
                data.addNotification(notify)
                StyleableToast.makeText(this, "Offer reported!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
            }
            else StyleableToast.makeText(this, "Error, please try later", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun deliver(){
        val hashMap = HashMap<String, Any>()
        hashMap.put("status","delivered")
        hashMap.put("deliveryDate",getCurrentDate())

        userBall += orderPrice
        val hashMap2 = HashMap<String, Any>()
        hashMap2.put("ballance",userBall)

        data.update(auth.currentUser!!.uid,hashMap2)

        val hashMap3 = HashMap<String, Any>()
        hashMap3.put("delivered",true)

        data.updateTransaction(transactionId,hashMap3)

        data.updateOrder(orderId,hashMap).addOnCompleteListener {
            if (it.isSuccessful) {
                val notify = NotificationObj(sellerID,customerID,"Order","Order is delivered","Your order has just been delivered!")
                data.addNotification(notify)
                StyleableToast.makeText(this, "Delivered!", Toast.LENGTH_SHORT,R.style.myToastUploaded).show()
                switchBack()
            }
            else StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun delete(position: Int){
        val file = files.get(position)
        data.getDatabaseReferenceOrder()!!.child(orderId).child("delivery_items").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snapshot1 in snapshot.children){
                    val fileDeliver = snapshot1.getValue(DeliveryFile::class.java)
                    if (fileDeliver?.path == file.path){
                        val refStorage = FirebaseStorage.getInstance().reference.child("delivery/$orderId/${fileDeliver.id}.png")
                        println("delivery/$orderId/${snapshot1.key}.png")
                        refStorage.delete()
                        snapshot1.ref.removeValue()
                        StyleableToast.makeText(applicationContext, "File Deleted!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        })
    }

    private fun rateOffer(rating:Float){
        val review = Review(rating,auth.currentUser!!.uid,"order",offerId,orderId,"")
        var counter = 0
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var r : Review? = null
                counter = 0
                for (dataP in dataSnapshot.children) {
                    r = dataP.getValue(Review::class.java)
                    if (r!!.orderId == orderId) counter++
                }
                if (counter == 0) submitReview(review)
                else updateReview(review,r!!)
                data.getDatabaseReferenceReview()!!.removeEventListener(this)
            }
            override fun onCancelled(error: DatabaseError) {
                StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                data.getDatabaseReferenceReview()!!.removeEventListener(this)
            }
        }
        data.getDatabaseReferenceReview()!!.orderByChild("fromId").equalTo(auth.currentUser!!.uid).addValueEventListener(listener)
    }

    private fun submitReview(review : Review){
        data.addReview(review).addOnCompleteListener {
            if (it.isSuccessful) {
                StyleableToast.makeText(this, "Saved!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
            }
            else StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun updateReview(review : Review,review2 : Review){
        val hashMap = HashMap<String, Any>()
        hashMap.put("rating",review.rating)
        hashMap.put("date",review.date)
        data.updateReview(review2.id,hashMap).addOnCompleteListener {
            if (it.isSuccessful) {
                StyleableToast.makeText(this, "Review Updated!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
            }
            else StyleableToast.makeText(this, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }



    private fun switchBack(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }


    // Get the result from this Override method
    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1001 ->
                    if (data != null) {
                        if (data.clipData != null) {
                            var index = 0
                            while (index < data.clipData!!.itemCount) {
                                val uri: Uri = data.clipData!!.getItemAt(index).uri
                                val i = index + 1
                                uploadFileToFirebase(uri,i)
                                index++
                            }
                        } else {
                            val uri: Uri? = data.data
                            uploadFileToFirebase(uri!!,0)
                        }
                    }
            }
        }
    }

    private fun uploadFileToFirebase(fileUri: Uri,index : Int) {
        var type = getMimeType(fileUri)
        type = type?.substringAfter("/")
        if (type == "png" || type == "jpeg" || type == "jpg" || type == "pdf" || type == "gif" || type == "txt"){
            dialog?.show()
            StyleableToast.makeText(this, "Upload Started!", Toast.LENGTH_SHORT, R.style.myToastDone).show();
            val id = UUID.randomUUID().toString()
            val fileName = "$orderId/$id.$type"

            val refStorage = FirebaseStorage.getInstance().reference.child("delivery/$fileName")

            refStorage.putFile(fileUri)
                .addOnSuccessListener(
                    OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                            val imageUrl = it.toString()
                            val deliveryFile = DeliveryFile(imageUrl,id,getCurrentDate(),type)
                            data.getDatabaseReferenceOrder()!!.child(orderId).child("delivery_items")
                                .push().setValue(deliveryFile)
                            dialog?.dismiss()

                            if (index > 0) StyleableToast.makeText(this, "$index File Uploaded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show();
                            else StyleableToast.makeText(this, "File Uploaded!", Toast.LENGTH_SHORT, R.style.myToastUploaded).show()
                        }
                    })
                .addOnFailureListener(OnFailureListener { e ->
                    StyleableToast.makeText(this, "Error, try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
                    dialog?.dismiss()
                })
        }
        else{
            StyleableToast.makeText(this, "Not supported type!", Toast.LENGTH_SHORT, R.style.myToastFail).show();
        }
    }

    private fun lockAllInput(){
        binding.recyclerDelivery.isClickable = locked
        locked = !locked
    }

    //Getting type of uploaded file
    private fun getMimeType(uri: Uri): String? {
        return contentResolver.getType(uri)
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