package app.code.xfp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.Order
import app.code.xfp.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import java.io.File
import java.util.Calendar

class OrderAdapter(private val list : ArrayList<Order>) : RecyclerView.Adapter<OrderAdapter.MyViewHolder>() {
    private lateinit var mlistener : OrderAdapter.onItemClickListener
    private var data = FirebaseData()

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_admin_order, parent,false)
        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = list[position]


        holder.title.text = currentitem.offerTitle
        if (currentitem.orderDate != ""){
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentitem.orderDate.toLong()
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
                    holder.customer.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.customerID).addListenerForSingleValueEvent(listener)

        val listener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    holder.seller.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.sellerID).addListenerForSingleValueEvent(listener2)

        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.customerID}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.customerImage.setImageBitmap(bitmap)
        }

        val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.sellerID}.png")
        var localfile2 = File.createTempFile("tempImage2","png")
        storageReference2.getFile(localfile2).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
            holder.sellerImage.setImageBitmap(bitmap)
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView : View,listener: OrderAdapter.onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val seller : TextView = itemView.findViewById(R.id.usernameSeller)
        val customer : TextView = itemView.findViewById(R.id.username)
        val title : TextView = itemView.findViewById(R.id.titleOrder)
        val date : TextView = itemView.findViewById(R.id.orderDate)
        val customerImage : AvatarView = itemView.findViewById(R.id.profileIcon)
        val sellerImage : AvatarView = itemView.findViewById(R.id.profileIcon2)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}