package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.adapters.NotificationAdapter
import app.code.xfp.databinding.NotificationBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.Offer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast
import kotlin.Comparator
import kotlin.collections.ArrayList

class Notifications : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private lateinit var binding: NotificationBinding


    private var notifications = ArrayList<NotificationObj>()
    private var showList = ArrayList<NotificationObj>()
    private var adapter : NotificationAdapter ?= null
    private var maxSearch = 10


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUser()
        adapter = NotificationAdapter(applicationContext,showList)

        binding = NotificationBinding.inflate(layoutInflater)

        binding.recyclerNot.adapter = adapter
        binding.recyclerNot.layoutManager = LinearLayoutManager(this)

        getNotifications()


        binding.recyclerNot.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    private fun getNotifications(){
        data.getDatabaseReferenceNotification()!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    showList.clear()
                    notifications.clear()
                    maxSearch = 10

                    val uid = auth.currentUser?.uid
                    for (i in snapshot.children) {
                        val notif = i.getValue(NotificationObj::class.java)
                        if (notif!!.toId == uid) {
                            notifications.add(notif)
                        }
                    }
                    val comparator = Comparator<NotificationObj> { obj1, obj2 ->
                        val date1 = obj1.date.toLongOrNull() ?: 0L
                        val date2 = obj2.date.toLongOrNull() ?: 0L
                        date2.compareTo(date1)
                    }
                    notifications.sortWith(comparator)
                    loadNextChunkOfData()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }




    private fun loadNextChunkOfData(){
        maxSearch += 10
        val newItems = java.util.ArrayList<NotificationObj>()
        var new : Boolean = false

        for (notify in notifications) {
            if (!showList.contains(notify)) {
                if (newItems.size >= maxSearch - showList.size) {
                    break
                }
                else{
                    new = true
                    newItems.add(notify)
                }
            }
        }
        if (new){
            val start = showList.size
            showList.addAll(newItems)
            adapter!!.notifyDataSetChanged()
        }
        else{
            maxSearch -= 10
        }
    }


    private fun checkCon(){
        if (notifications.isEmpty()){
            binding.recyclerNot.visibility = View.GONE
            binding.bottomH.visibility = View.VISIBLE
        }
        if (notifications.isNotEmpty() && binding.recyclerNot.visibility == View.GONE){
            binding.recyclerNot.visibility = View.VISIBLE
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