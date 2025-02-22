package app.code.xfp

import android.app.Dialog
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.adapters.TransactionAdapter
import app.code.xfp.databinding.WalletBinding
import app.code.xfp.objects.TransactionClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.github.muddz.styleabletoast.StyleableToast
import java.util.ArrayList
import java.util.Calendar
import java.util.UUID


class Wallet : AppCompatActivity() {
    private lateinit var binding: WalletBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data : FirebaseData = FirebaseData()

    private val list = ArrayList<TransactionClass>()
    private var showList = ArrayList<TransactionClass>()
    private var adapter : TransactionAdapter = TransactionAdapter(showList)
    private var maxSearch : Int = 3

    private var isLoading = false
    private val handler = Handler(Looper.getMainLooper())
    private var dialog : Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        checkUser()
        binding = WalletBinding.inflate(layoutInflater)

        binding.recyclerTrans.adapter = adapter
        binding.recyclerTrans.layoutManager = LinearLayoutManager(this)

        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(app.code.xfp.R.layout.dialog_loading)
        dialog!!.setCanceledOnTouchOutside(false)


        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    var map = dataP.getValue() as Map<*,*>
                    binding.name.text = map.get("name").toString()
                    binding.ball.text = map.get("ballance").toString()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener)

        getData()

        binding.recyclerTrans.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

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

        binding.addFunds.setOnClickListener {
            dialog?.show()
            binding.addFunds.isEnabled = false
            var dailyList = ArrayList<TransactionClass>()

            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val transaction = snapshot.getValue(TransactionClass::class.java)

                        if (transaction?.from == "Daily Bonus"){
                            dailyList.add(transaction)
                        }
                    }
                    checkDate(dailyList)
                }
                override fun onCancelled(error: DatabaseError) {
                    dialog?.dismiss()
                    binding.addFunds.isEnabled = true
                }
            }
            data.getDatabaseReferenceTransaction()!!.orderByChild("to").equalTo(auth.currentUser!!.uid).addListenerForSingleValueEvent(listener2)
        }


        setContentView(binding.root)
    }

    private fun Calendar.isSameDayAs(other: Calendar): Boolean {
        return this.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR) &&
                this.get(Calendar.YEAR) == other.get(Calendar.YEAR)
    }

    private fun checkDate(dailyList : ArrayList<TransactionClass>){
        val comparator = Comparator<TransactionClass> { obj1, obj2 ->
            val date1 = obj1.date.toLongOrNull() ?: 0L
            val date2 = obj2.date.toLongOrNull() ?: 0L
            date2.compareTo(date1)
        }
        dailyList.sortWith(comparator)

        val today = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
        var found : Boolean = false

        for (transaction in dailyList){
            val cal = Calendar.getInstance()
            cal.timeInMillis = transaction.date.toLong()
            val date = DateFormat.format("yyyy-MM-dd HH:mm:ss", cal.time).toString()
            val isToday = cal.isSameDayAs(today)

            if (isToday) {
                dialog?.dismiss()
                binding.addFunds.isEnabled = true
                StyleableToast.makeText(applicationContext, "Daily bonus has been claimed today!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                found = true
            }
        }

        if (!found){
            var transaction = TransactionClass("Daily Bonus",auth.currentUser!!.uid,"15","Daily Bonus",System.currentTimeMillis().toString(),UUID.randomUUID().toString(),true)
            data.addTransaction(transaction).addOnCompleteListener {
                if (it.isSuccessful){
                    dialog?.dismiss()
                    binding.addFunds.isEnabled = true
                    StyleableToast.makeText(applicationContext, "Daily bonus claimed !", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                }
                else{
                    dialog?.dismiss()
                    binding.addFunds.isEnabled = true
                    StyleableToast.makeText(applicationContext, "Error, please try later!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
                }
            }
        }
    }


    private fun getData(){
        data.getDatabaseReferenceTransaction()!!.orderByChild("date").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    list.clear()
                    showList.clear()
                    maxSearch = 3
                    for (i in snapshot.children) {
                        val transaction = i.getValue(TransactionClass::class.java)
                        if (transaction?.to == auth.currentUser?.uid && transaction!!.delivered || transaction?.from == auth.currentUser?.uid){
                            list.add(transaction!!)
                        }
                    }

                    val comparator = Comparator<TransactionClass> { obj1, obj2 ->
                        val date1 = obj1.date.toLongOrNull() ?: 0L
                        val date2 = obj2.date.toLongOrNull() ?: 0L
                        date2.compareTo(date1)
                    }
                    list.sortWith(comparator)

                    loadNextChunkOfData()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadNextChunkOfData(){
        maxSearch += 5
        val newItems = ArrayList<TransactionClass>()
        var new : Boolean = false

        for (trans in list) {
            if (!showList.contains(trans)) {
                if (newItems.size >= maxSearch - showList.size) {
                    break
                }
                else{
                    new = true
                    newItems.add(trans)
                }
            }
        }
        if (new){
            showList.addAll(newItems)
            maxSearch = showList.size
            adapter.notifyDataSetChanged()
        }
        else{
            maxSearch -= 5
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