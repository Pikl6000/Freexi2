package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ReportAdapter
import app.code.xfp.adapters.ReviewAdapter
import app.code.xfp.databinding.OfferDetailBinding
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

class OfferDetail : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: OfferDetailBinding
    private var sellerId : String = ""
    private var offerId : String = ""

    private var list = ArrayList<Review>()
    private var adapter : ReviewAdapter = ReviewAdapter(list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = OfferDetailBinding.inflate(layoutInflater)

        binding.recyclerReviews.adapter = adapter
        binding.recyclerReviews.layoutManager = LinearLayoutManager(this)


        //Getting data from Bundle and Setting them
        val bundle : Bundle?= intent.extras
        sellerId = bundle!!.getString("userId").toString()
        offerId = bundle.getString("id").toString()

        val desc = Html.fromHtml(bundle.getString("description"))
        val descS = Html.fromHtml(bundle.getString("descriptionS"))

        binding.offerTitle.text = bundle.getString("title")
        binding.offerDescriptionS.text = descS
        binding.offerDescription.text = desc
        binding.ratingBar.rating = bundle.get("rating") as Float
        binding.price.text = bundle.getString("price")

        getReviews()


        val storageReference = FirebaseStorage.getInstance().reference.child("offer_images/"+bundle.getString("image"))
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.imageView.setImageBitmap(bitmap)
        }

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)

                    if (user!!.id.equals(sellerId)){
                        binding.fromT.text = user.country
                        val desc = Html.fromHtml(user.description)
                        binding.aboutT.text = desc
                        binding.creator2.text = user.name

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
                        data.getDatabaseReference()!!.removeEventListener(this)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                data.getDatabaseReference()!!.removeEventListener(this)
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(sellerId).addListenerForSingleValueEvent(listener)

        //Showing sellers account
        binding.creator2.setOnClickListener{
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("name",binding.creator2.text)
            intent.putExtra("sellerId",sellerId)
            startActivity(intent)
        }
        binding.profileIcon.setOnClickListener{
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("name",binding.creator2.text)
            intent.putExtra("sellerId",sellerId)
            startActivity(intent)
        }

        //Goint to confirmation screen
        binding.order.setOnClickListener{
            val intent = Intent(this, SendOrder::class.java)
            intent.putExtra("title",binding.offerTitle.text)
            intent.putExtra("price",binding.price.text)
            intent.putExtra("seller",binding.creator2.text)
            intent.putExtra("sellerId",sellerId)
            intent.putExtra("id",bundle.getString("id"))
            startActivity(intent)
        }

        binding.report.setOnClickListener{
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Report offer")
            builder.setCancelable(true)

            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.alert_dialog_message, null)
            val input = dialogView.findViewById<TextInputEditText>(R.id.textInputEditTextA1)

            builder.setView(dialogView)

            builder.setPositiveButton("Report") { dialog, which ->
                checkReport(auth.currentUser?.uid.toString(),bundle.getString("id").toString(),input.text.toString().trim())
            }

            builder.setNegativeButton("Cancel") { dialog, which ->

            }
            builder.show()
        }

        setContentView(binding.root)
    }


    private fun getReviews(){
        data.getDatabaseReferenceReview()!!.orderByChild("toId").equalTo(offerId).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var l = ArrayList<Review>()
                if (snapshot.exists()) {
                    list.clear()
                    for (i in snapshot.children) {
                        val review = i.getValue(Review::class.java)
                        list.add(review!!)
                    }
                    val comparator = Comparator<Review> { obj1, obj2 ->
                        val date1 = obj1.date.toLongOrNull() ?: 0L
                        val date2 = obj2.date.toLongOrNull() ?: 0L
                        date2.compareTo(date1)
                    }
                    l.sortWith(comparator)

                    var counter = 0
                    for (review in l){
                        if (counter < 5){
                            list.add(review)
                            counter++
                        }
                    }

                    var count = 0
                    var rating = 0F
                    for (review in l){
                        count++
                        rating += review.rating
                    }
                    binding.ratingBar.rating = rating/count

                    adapter.notifyDataSetChanged()
                }
                if (list.isEmpty()) binding.textView28.text = getString(R.string.no_reviews)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun reportOffer(userId : String,offerId : String,message : String){
        var report : Report = Report("Offer",System.currentTimeMillis().toString(),offerId,message,userId,false)
        data.addReport(report).addOnCompleteListener {
            if (it.isSuccessful) {
                val notify = NotificationObj(userId,sellerId,"warning","Offer Report","Your offer has been reported!")
                data.addNotification(notify)
                Log.v("Report","Offer reported")
                StyleableToast.makeText(this, "Offer reported!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
            }
            else StyleableToast.makeText(this, "Error, please try later", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
    }

    private fun checkReport(userId : String,offerId : String,message : String){
        if (message.isEmpty()){
            StyleableToast.makeText(applicationContext, "You have to enter message!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
        }
        else{
            val id = userId+offerId
            data.getDatabaseReferenceReport()!!.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var found = false
                    if (snapshot.exists()) {
                        for (i in snapshot.children) {
                            val report = i.getValue(Report::class.java)
                            if (report!!.reportedId == offerId) {
                                StyleableToast.makeText(applicationContext, "You already reported this offer!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                                found = true
                            }
                        }
                       if (!found) {
                           reportOffer(userId, offerId, message)
                           Log.v("Report","Creating report")
                           data.getDatabaseReferenceReport()!!.removeEventListener(this)
                       }
                       else Log.v("Report error","Report already created")
                        data.getDatabaseReferenceReport()!!.removeEventListener(this)
                    }
                    else{
                        reportOffer(userId, offerId, message)
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