package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.UserProfileBinding
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
import java.util.*

class UserProfile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: UserProfileBinding
    private var uid = ""

    val offerList = ArrayList<Offer>()
    private var adapter = ViewAdapter(offerList)
    private var sellerId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = UserProfileBinding.inflate(layoutInflater)

        //Getting data from Bundle
        val bundle : Bundle?= intent.extras

        sellerId = bundle!!.getString("sellerId")

        binding.recyclerOffer.adapter = adapter
        binding.recyclerOffer.layoutManager = LinearLayoutManager(this)

        if (auth.currentUser!!.uid == sellerId){
            binding.contact.visibility = View.GONE
            binding.report.visibility = View.GONE
        }

        adapter.setOnItemClickListener(object : ViewAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext,OfferDetail::class.java)
                val offer = offerList.get(position)
                intent.putExtra("title",offer.title)
                intent.putExtra("image",offer.pathImage)
                intent.putExtra("id",offer.id)
                intent.putExtra("rating",offer.rating.toFloat())
                intent.putExtra("description",offer.description)
                intent.putExtra("descriptionS",offer.smallDescription)
                intent.putExtra("price",offer.price.toString())
                intent.putExtra("userId",offer.sellerId)
                startActivity(intent)
            }
        })

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)

                    if (user!!.id.equals(sellerId)){
                        //binding.ratingBarUser.rating = user!!.rating.toFloat()

                        if (user.name == "DELETED USER" && user.ballance == -1 ){
                            StyleableToast.makeText(applicationContext, "User not found!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                            val intent = Intent(applicationContext, NoAccount::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }

                        binding.fromT.text = user.country
                        binding.userDescription.text = user.description
                        binding.seller.text = user.name
                        uid = user.id

                        if (user.joinDate != ""){
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = user.joinDate.toLong()
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            val month = calendar.get(Calendar.MONTH) + 1
                            val year = calendar.get(Calendar.YEAR)
                            val time = "$day.$month.$year"
                            binding.joinedT.text = time
                        }

                        getData()

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

        binding.contact.setOnClickListener {
            val chatRoom : ChatRoomObj = ChatRoomObj(auth.currentUser!!.uid,sellerId!!)
            val chatRoomS : ChatRoomObj = ChatRoomObj(sellerId!!,auth.currentUser!!.uid)
            data.addChatRoom(chatRoom).addOnSuccessListener {
                data.addChatRoom(chatRoomS).addOnSuccessListener {
                    val intent = Intent(this, ChatSelection::class.java)
                    startActivity(intent)
                }
            }
        }

        binding.report.setOnClickListener {
            binding.report.setOnClickListener{
                val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
                builder.setTitle("Report User")
                builder.setCancelable(true)

                val inflater = layoutInflater
                val dialogView = inflater.inflate(R.layout.alert_dialog_message, null)
                val input = dialogView.findViewById<TextInputEditText>(R.id.textInputEditTextA1)

                builder.setView(dialogView)

                builder.setPositiveButton("Report") { dialog, which ->
                    checkReport(auth.currentUser?.uid.toString(),sellerId!!,input.text.toString().trim())
                }

                builder.setNegativeButton("Cancel") { dialog, which ->

                }
                builder.show()
            }
        }

        setContentView(binding.root)
    }

    private fun getData(){
        checkInternet()
        data.getDatabaseReferenceOffer()!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    offerList.clear()
                    for (i in snapshot.children) {
                        val offer = i.getValue(Offer::class.java)
                        if (offer!!.sellerId == uid) {
                            offerList.add(offer)
                        }
                    }
                    adapter.notifyDataSetChanged()

                    if (offerList.isEmpty()){
                        binding.recyclerOffer.visibility = View.GONE
                        binding.textView59.visibility = View.VISIBLE
                    }
                    else{
                        binding.recyclerOffer.visibility = View.VISIBLE
                        binding.textView59.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
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
                            reportUser(userId, reportedId, message)
                            Log.v("Report","Creating report")
                            data.getDatabaseReferenceReport()!!.removeEventListener(this)
                        }
                        else Log.v("Report error","Report already created")
                        data.getDatabaseReferenceReport()!!.removeEventListener(this)
                    }
                    else{
                        reportUser(userId, reportedId, message)
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

    private fun reportUser(userId : String,offerId : String,message : String){
        var report : Report = Report("User",System.currentTimeMillis().toString(),offerId,message,userId,false)
        data.addReport(report).addOnCompleteListener {
            if (it.isSuccessful) {
                val notify = NotificationObj(userId,sellerId!!,"warning","User Report","You have been reported!")
                data.addNotification(notify)
                StyleableToast.makeText(this, "Offer reported!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
            }
            else StyleableToast.makeText(this, "Error, please try later", Toast.LENGTH_SHORT, R.style.myToastFail).show()
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