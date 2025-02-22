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
import app.code.xfp.*
import app.code.xfp.adapters.UserAdapter
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.AdminManageOfferBinding
import app.code.xfp.databinding.AdminManageUserBinding
import app.code.xfp.objects.Offer
import app.code.xfp.objects.Review
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.util.ArrayList

class AdminOffers : AppCompatActivity() {
    private var data : FirebaseData = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding : AdminManageOfferBinding

    private var selected : Int = 0
    private var selectedId : String = ""
    private var list = ArrayList<Offer>()
    private var showList = ArrayList<Offer>()
    private var adapter = ViewAdapter(list)
    private var maxSearch : Int = 3
    private var possId : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminManageOfferBinding.inflate(layoutInflater)
        checkUser()

        val bundle : Bundle?= intent.extras
        possId = bundle?.getString("id").toString()

        if (possId != "null"){
            selectedId = possId
            changeScene(possId)
        }

        adapter.setOnItemClickListener(object : ViewAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                selected = position
                selectedId = list.get(position).id
                changeScene()
            }
        })

        binding.recyclerResultOffer.adapter = adapter
        binding.recyclerResultOffer.layoutManager = LinearLayoutManager(this)

        binding.editInformation.setOnClickListener {
            val intent = Intent(this, ManageOffer::class.java)
            intent.putExtra("id",selectedId)
            startActivity(intent)
        }

        binding.deleteOffer.setOnClickListener {
            val builder = AlertDialog.Builder(this,R.style.AlertDialogTheme)
            builder.setTitle("Confirm action")
            builder.setCancelable(true)
            builder.setMessage("Are you sure you want to delete this offer ?")

            builder.setPositiveButton("Delete") { dialog, which ->
                 deleteOffer()
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
            }
            builder.show()
        }


        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchOffers(query.trim(),maxSearch)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchOffers(newText.trim(),maxSearch)
                return false
            }
        })

        binding.sellerAcc.setOnClickListener {
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("sellerId",list.get(selected).sellerId)
            startActivity(intent)
        }

        binding.recyclerResultOffer.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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


    private fun searchOffers(query: String, max: Int) {
        val searchQuery = data.getDatabaseReferenceOffer()!!.orderByChild("title").startAt(query).endAt(query + "\uf8ff")

        searchQuery.addValueEventListener(object : ValueEventListener {
            var counter : Int = 0
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (list.isNotEmpty()) list.clear()
                for (snapshot in dataSnapshot.children) {
                    val offer = snapshot.getValue(Offer::class.java)
                    if (counter < max) {
                        list.add(offer!!)
                        counter++
                    }
                }
                loadNextChunkOfData()
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }


    private fun loadNextChunkOfData(){
        maxSearch += 3
        val newItems = ArrayList<Offer>()
        var new : Boolean = false

        if (showList.size > list.size){
            showList.clear()
        }

        for (offer in list) {
            if (!showList.contains(offer)) {
                if (newItems.size >= maxSearch - showList.size) {
                    break
                }
                else{
                    new = true
                    newItems.add(offer)
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
        binding.searchUser.visibility = View.GONE
        binding.resultUser.visibility = View.VISIBLE

        var searchId = list.get(selected).id

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val offer = dataP.getValue(Offer::class.java)
                    binding.offerTitle.text = offer!!.title

                    val desc = Html.fromHtml(offer.description)
                    val descS = Html.fromHtml(offer.smallDescription)

                    binding.offerDescriptionS.text = descS
                    binding.offerDescription.text = desc
                    binding.price.text = offer.price.toString()

                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)


        val storageReference = FirebaseStorage.getInstance().reference.child("offer_images/${searchId}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            Glide.with(this)
                .load(bitmap)
                .into(binding.imageView)

        }.addOnFailureListener{
            Glide.with(this)
                .load(R.drawable.person)
                .into(binding.imageView)
        }
    }

    private fun changeScene(searchId : String){
        binding.searchUser.visibility = View.GONE
        binding.resultUser.visibility = View.VISIBLE

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val offer = dataP.getValue(Offer::class.java)
                    binding.offerTitle.text = offer!!.title

                    val desc = Html.fromHtml(offer.description)
                    val descS = Html.fromHtml(offer.smallDescription)

                    binding.offerDescriptionS.text = descS
                    binding.offerDescription.text = desc
                    binding.price.text = offer.price.toString()

                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(searchId).addListenerForSingleValueEvent(listener)


        val storageReference = FirebaseStorage.getInstance().reference.child("offer_images/${searchId}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            Glide.with(this)
                .load(bitmap)
                .into(binding.imageView)

        }.addOnFailureListener{
            Glide.with(this)
                .load(R.drawable.person)
                .into(binding.imageView)
        }
    }

    private fun deleteOffer(){
        data.getDatabaseReferenceOffer()!!.child(selectedId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snapshot1 in snapshot.children){
                    val refStorage = FirebaseStorage.getInstance().reference.child("offer_images/${selectedId}.png")
                    refStorage.delete()
                    snapshot1.ref.removeValue()
                    StyleableToast.makeText(applicationContext, "Offer Deleted!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
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