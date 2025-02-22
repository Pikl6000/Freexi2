package app.code.xfp.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.*
import app.code.xfp.adapters.ReportAdapter
import app.code.xfp.adapters.UserAdapter
import app.code.xfp.databinding.AdminPanelBinding
import app.code.xfp.databinding.AdminReportActionBinding
import app.code.xfp.databinding.AdminReportsBinding
import app.code.xfp.objects.*
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.util.ArrayList

class Reports : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private lateinit var binding : AdminReportsBinding
    private lateinit var binding2 : AdminReportActionBinding

    private var list = ArrayList<Report>()
    private lateinit var adapter : ReportAdapter
    private var Listposition : Int = 0
    private var seen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = AdminReportsBinding.inflate(layoutInflater)
        binding2 = AdminReportActionBinding.inflate(layoutInflater)

        adapter = ReportAdapter(list)
        adapter.setOnItemClickListener(object : ReportAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                Listposition = position
                changeScene(Listposition)
            }
        })

        binding.recyclerResultReports.adapter = adapter
        binding.recyclerResultReports.layoutManager = LinearLayoutManager(this)

        getData()

        binding2.card.setOnClickListener{
            if (list.get(Listposition).type.equals("Offer")){
                val listener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (dataP in dataSnapshot.children) {
                            val offer = dataP.getValue(Offer::class.java)
                            openOffer(offer)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                }
                data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(list.get(Listposition).reportedId).addListenerForSingleValueEvent(listener)


            }
            else{
                val intent = Intent(this, UserProfile::class.java)
                intent.putExtra("sellerId",list.get(Listposition).reportedId)
                startActivity(intent)
            }
        }

        binding2.actionButton.setOnClickListener {
            if (list.get(Listposition).type.equals("Offer")){
                val intent = Intent(this, AdminOffers::class.java)
                intent.putExtra("id",list.get(Listposition).reportedId)
                startActivity(intent)
            }
            else if (list.get(Listposition).type.equals("User")){
                val intent = Intent(this, AdminUsers::class.java)
                intent.putExtra("id",list.get(Listposition).reportedId)
                startActivity(intent)
            }
            else if (list.get(Listposition).type.equals("Support")){
                val chatRoom : ChatRoomObj = ChatRoomObj(auth.currentUser!!.uid,list.get(Listposition).userId)
                val chatRoomS : ChatRoomObj = ChatRoomObj(list.get(Listposition).userId,auth.currentUser!!.uid)
                data.addChatRoom(chatRoom).addOnSuccessListener {
                    data.addChatRoom(chatRoomS).addOnSuccessListener {
                        val intent = Intent(applicationContext,ChatRoom::class.java)
                        intent.putExtra("receiverUid",chatRoom.receiverUid)
                        intent.putExtra("senderUid",chatRoom.senderUid)
                        startActivity(intent)
                    }
                }
            }
            else if (list.get(Listposition).type.equals("Order")){
                val intent = Intent(this, AdminOrders::class.java)
                intent.putExtra("id",list.get(Listposition).reportedId)
                startActivity(intent)
            }
            else{
                StyleableToast.makeText(this, "WORKING ON IT!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        }

        binding2.dismissButton.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm action")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to delete this report ?")

            builder.setPositiveButton("Delete") { dialog, which ->
                deleteReport()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }


        setContentView(binding.root)
    }

    private fun deleteReport(){
        val hashMap = HashMap<String, Any>()
        hashMap.put("accept",true)

        var reportID : String = ""
        if (list.get(Listposition).type == "Support"){
            reportID = list.get(Listposition).id
        }
        else{
            reportID = list.get(Listposition).userId + list.get(Listposition).reportedId
        }

        data.getDatabaseReferenceReport()!!.child(reportID).removeValue().addOnCompleteListener{
            if (it.isSuccessful){
                if (list.get(Listposition).type.equals("Offer")){
                    val listener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (dataP in dataSnapshot.children) {
                                val offer = dataP.getValue(Offer::class.java)
                                val notify = NotificationObj(auth.currentUser!!.uid,offer!!.sellerId,"warning","Admin action","Your reported offer ${offer.title} has been resolved!")
                                data.addNotification(notify)

                                val intent = Intent(applicationContext, AdminPanel::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    }
                    data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(list.get(Listposition).reportedId).addListenerForSingleValueEvent(listener)
                }
                else if (list.get(Listposition).type.equals("User")){
                    val notify = NotificationObj(auth.currentUser!!.uid,list.get(Listposition).reportedId,"warning","Admin action","Your account report has been resolved!")
                    data.addNotification(notify)

                    val intent = Intent(applicationContext, AdminPanel::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                else if (list.get(Listposition).type.equals("Support")){
                    val notify = NotificationObj(auth.currentUser!!.uid,list.get(Listposition).userId,"warning","Admin action","Your support ticket has been closed!")
                    data.addNotification(notify)

                    val intent = Intent(applicationContext, AdminPanel::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (seen){
            seen = false
            setContentView(binding.root)
        }
        else{
            super.onBackPressed()
        }
    }

    private fun openOffer(offer: Offer?){
        val intent = Intent(this, OfferDetail::class.java)
        intent.putExtra("title", offer?.title)
        intent.putExtra("image",offer?.pathImage)
        intent.putExtra("id",offer?.id)
        intent.putExtra("rating",offer?.rating)
        intent.putExtra("description",offer?.description)
        intent.putExtra("descriptionS",offer?.smallDescription)
        intent.putExtra("price",offer?.price.toString())
        intent.putExtra("userId",offer?.sellerId)
        startActivity(intent)
    }

    private fun changeScene(position: Int){
        //Loading information about report
        binding2.title.text = list.get(position).type + " report"
        binding2.name.text = list.get(position).reportedId
        binding2.reportMessage.text = list.get(position).message

        //Loading information about user
        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${list.get(position).userId}.png")
        var localfile = File.createTempFile("tempImage2","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding2.profileIcon.setImageBitmap(bitmap)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    binding2.username.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(list.get(position).userId).addListenerForSingleValueEvent(listener)


        if (list.get(Listposition).type.equals("Offer")){
            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(Offer::class.java)
                        binding2.name.text = user!!.title
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(list.get(position).reportedId).addListenerForSingleValueEvent(listener2)
        }
        else if (list.get(Listposition).type.equals("User")){
            val listener3 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        binding2.name.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(list.get(position).reportedId).addListenerForSingleValueEvent(listener3)
        }
        else if (list.get(Listposition).type.equals("Support")){
            binding2.reportedCart.visibility = View.GONE
            binding2.messageCon.setBackgroundResource(R.drawable.account_borders)
            binding2.textView21.setText(R.string.submitted_by)
        }

        seen = true
        setContentView(binding2.root)
    }


    private fun getData(){
        data.getDatabaseReferenceReport()!!.orderByChild("date").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    list.clear()
                    for (i in snapshot.children) {
                        val report = i.getValue(Report::class.java)
                        if (!report!!.accept){
                            list.add(report!!)
                        }
                    }
                    list.reverse()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
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