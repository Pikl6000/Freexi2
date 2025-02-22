package app.code.xfp

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.WorkAdapter
import app.code.xfp.databinding.DeliveriesBinding
import app.code.xfp.objects.Order
import app.code.xfp.objects.TransactionClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast
import java.util.ArrayList

class Deliveries : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: DeliveriesBinding

    private val list = ArrayList<Order>()
    private var adapter : WorkAdapter = WorkAdapter(list)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = DeliveriesBinding.inflate(layoutInflater)

        adapter.setOnItemClickListener(object : WorkAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext,DeliverOrder::class.java)
                val order = list.get(position)
                intent.putExtra("orderId",order.id)
                startActivity(intent)
            }
        })

        binding.recyclerDelivered.adapter = adapter
        binding.recyclerDelivered.layoutManager = LinearLayoutManager(this)



        getData()

        setContentView(binding.root)
    }


    private fun getData(){
        data.getDatabaseReferenceOrder()!!.orderByChild("orderDate").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    list.clear()

                    for (i in snapshot.children) {
                        val order = i.getValue(Order::class.java)
                        if (order!!.sellerID == auth.currentUser!!.uid || order.customerID == auth.currentUser!!.uid ||
                            order.customerID == auth.currentUser!!.uid ||order.sellerID == auth.currentUser!!.uid){
                            if (!list.contains(order)) list.add(order)
                        }
                    }
                    val comparator = Comparator<Order> { obj1, obj2 ->
                        val date1 = obj1.orderDate.toLongOrNull() ?: 0L
                        val date2 = obj2.orderDate.toLongOrNull() ?: 0L
                        date2.compareTo(date1)
                    }
                    list.sortWith(comparator)
                    adapter.notifyDataSetChanged()
                }
                checkCon()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkCon(){
        if (list.isEmpty()){
            binding.scrollDelivered.visibility = View.GONE
            binding.textView5.visibility = View.GONE
            binding.bottomH.visibility = View.VISIBLE
        }
        if (list.isNotEmpty() && binding.scrollDelivered.visibility == View.GONE){
            binding.scrollDelivered.visibility = View.VISIBLE
            binding.textView5.visibility = View.VISIBLE
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