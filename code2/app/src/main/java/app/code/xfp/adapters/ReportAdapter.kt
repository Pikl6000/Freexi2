package app.code.xfp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.Offer
import app.code.xfp.objects.Report
import app.code.xfp.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class ReportAdapter(private val list : ArrayList<Report>) : RecyclerView.Adapter<ReportAdapter.MyViewHolder>() {
    private lateinit var mlistener : onItemClickListener
    private var data = FirebaseData()

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_reports, parent,false)
        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = list[position]

        if (currentitem.type == "Support"){
            holder.title.text = currentitem.type + " Ticket"
            holder.mess.visibility = View.GONE
            holder.report.setText(R.string.submitted_by)
        }
        else{
            holder.title.text = currentitem.type +" Report"
        }

        if (currentitem.date != ""){
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentitem.date.toLong()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val time = "$day.$month.$year"
            holder.date.text = time
        }

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    holder.reporter.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.userId).addListenerForSingleValueEvent(listener)

        if (currentitem.type == "User"){
            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        holder.reported.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.reportedId).addListenerForSingleValueEvent(listener2)
        }
        if (currentitem.type == "Offer"){
            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val offer = dataP.getValue(Offer::class.java)
                        holder.reported.text = offer!!.title
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReferenceOffer()!!.orderByChild("id").equalTo(currentitem.reportedId).addListenerForSingleValueEvent(listener2)
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView : View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val reported : TextView = itemView.findViewById(R.id.reported)
        val reporter : TextView = itemView.findViewById(R.id.reporter)
        val date : TextView = itemView.findViewById(R.id.dateInfo)
        val title : TextView = itemView.findViewById(R.id.title)

        val mess : ConstraintLayout = itemView.findViewById(R.id.info1)
        val report : TextView = itemView.findViewById(R.id.textView47)

        init{
            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }
        }
    }
}