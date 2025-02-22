package app.code.xfp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.databinding.RecyclerTransactionBinding
import app.code.xfp.databinding.RecyclerTransactionPayBinding
import app.code.xfp.objects.TransactionClass
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class TransactionAdapter(private val list : ArrayList<TransactionClass>) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private var data = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    val RECEIVED = 2
    val PAID = 1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == PAID){
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_transaction_pay, parent,false)
            payViewHolder(itemView)
        }
        else{
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_transaction, parent,false)
            receiveViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentitem = list[position]

        if (holder.javaClass == payViewHolder::class.java){
            val viewHolder = holder as payViewHolder

            //Getting information from database and putting showing it
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.toValue.text = user?.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.to).addListenerForSingleValueEvent(listener)

            if (currentitem.date != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.date.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val time = "$day.$month.$year, $hour:$minute"
                viewHolder.binding.dateInfo.text = time
            }

            viewHolder.binding.amountValue.text = currentitem.price
        }
        else {
            val viewHolder = holder as receiveViewHolder


            if (currentitem.from == "Daily Bonus"){
                viewHolder.binding.fromValue.text = "Daily Bonus"
            }
            else{
                viewHolder.binding.fromValue.text = "Deleted User"
            }

            //Getting information from database and putting showing it
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.fromValue.text = user?.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.from).addListenerForSingleValueEvent(listener)

            if (currentitem.date != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.date.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val time = "$day.$month.$year, $hour:$minute"
                viewHolder.binding.dateInfo.text = time
            }
            viewHolder.binding.amountValue.text = currentitem.price
        }
    }

    override fun getItemViewType(position: Int): Int {
        val listItem = list[position]

        return if (listItem.to == auth.currentUser?.uid){
            RECEIVED
        }
        else{
            PAID
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class payViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var binding: RecyclerTransactionPayBinding = RecyclerTransactionPayBinding.bind(itemView)
    }
    inner class receiveViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var binding: RecyclerTransactionBinding = RecyclerTransactionBinding.bind(itemView)
    }
}