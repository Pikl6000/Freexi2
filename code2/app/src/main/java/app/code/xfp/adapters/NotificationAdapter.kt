package app.code.xfp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.databinding.RecyclerNotificationBinding
import app.code.xfp.databinding.RecyclerNotificationNBinding
import app.code.xfp.databinding.RecyclerTransactionBinding
import app.code.xfp.databinding.RecyclerTransactionPayBinding
import app.code.xfp.objects.NotificationObj
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class NotificationAdapter(context : Context, private val list : ArrayList<NotificationObj>) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private var data = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    val ERROR = 2
    val NOT = 1
    var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ERROR){
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_notification, parent,false)
            errorViewHolder(itemView)
        }
        else{
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_notification_n, parent,false)
            notificationViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentitem = list[position]

        if (holder.javaClass == errorViewHolder::class.java){
            val viewHolder = holder as errorViewHolder

            viewHolder.binding.message.text = currentitem.text
            viewHolder.binding.title.text = currentitem.title

            if (currentitem.date != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.date.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val time = "$day.$month.$year"
                viewHolder.binding.dateInfo.text = time
            }
        }
        else {
            val viewHolder = holder as notificationViewHolder

            viewHolder.binding.message.text = currentitem.text
            viewHolder.binding.title.text = currentitem.title

            if (currentitem.date != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.date.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val time = "$day.$month.$year"
                viewHolder.binding.dateInfo.text = time
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val listItem = list[position]

        return if (listItem.channel == "warning"){
            ERROR
        }
        else{
            NOT
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class errorViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var binding: RecyclerNotificationBinding = RecyclerNotificationBinding.bind(itemView)
    }
    inner class notificationViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var binding: RecyclerNotificationNBinding = RecyclerNotificationNBinding.bind(itemView)
    }
    init {
        this.context = context
    }
}