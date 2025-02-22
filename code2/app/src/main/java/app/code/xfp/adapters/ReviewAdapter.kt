package app.code.xfp.adapters

import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.Report
import app.code.xfp.objects.Review
import app.code.xfp.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class ReviewAdapter(private val list : ArrayList<Review>) : RecyclerView.Adapter<ReviewAdapter.MyViewHolder>() {
    private var data = FirebaseData()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_review, parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = list[position]

        val desc = Html.fromHtml(currentitem.text)
        holder.message.text = desc
        holder.rating.rating = currentitem.rating

        if (currentitem.date != ""){
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentitem.date.toLong()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val time = "$day.$month.$year"
            holder.date.text = time
        }

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    holder.reviewer.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.fromId).addListenerForSingleValueEvent(listener)
    }


    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val reviewer : TextView = itemView.findViewById(R.id.username)
        val date : TextView = itemView.findViewById(R.id.dateInfo)
        val rating : RatingBar = itemView.findViewById(R.id.ratingBar)
        val message : TextView = itemView.findViewById(R.id.reviewMessage)

    }
}