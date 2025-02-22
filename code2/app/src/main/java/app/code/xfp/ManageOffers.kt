package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.ManageOffersBinding
import app.code.xfp.objects.Offer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast
import java.util.ArrayList

class ManageOffers : AppCompatActivity() {
    private lateinit var binding: ManageOffersBinding
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    var img = false
    private var imgpath: Uri = "".toUri()
    private var imagepath: String = ""
    private var uid: String = ""


    val offerList = ArrayList<Offer>()
    private var adapter : ViewAdapter = ViewAdapter(offerList)

    companion object {
        val IMAGE_REQUEST_CODE = 1_000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageOffersBinding.inflate(layoutInflater)

        checkInternet()

        auth = FirebaseAuth.getInstance()

        checkUser()
        uid = auth.currentUser!!.uid
        getData()

        adapter.setOnItemClickListener(object : ViewAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext,ManageOffer::class.java)
                val offer = offerList.get(position)
                intent.putExtra("id",offer.id)
                startActivity(intent)
            }
        })

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)

        setContentView(binding.root)
    }

    private fun getData() {
        data.getDatabaseReferenceOffer()!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (i in snapshot.children) {
                        val offer = i.getValue(Offer::class.java)
                        if (offer!!.sellerId == uid) {
                            offerList.add(offer)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                checkCon()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkCon(){
        if (offerList.isEmpty()){
            binding.recyclerView.visibility = View.GONE
            binding.bottomH.visibility = View.VISIBLE
        }
        if (offerList.isNotEmpty() && binding.recyclerView.visibility == View.GONE){
            binding.recyclerView.visibility = View.VISIBLE
            binding.bottomH.visibility = View.GONE
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