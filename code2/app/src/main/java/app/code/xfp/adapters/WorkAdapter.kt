package app.code.xfp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.databinding.RecyclerAwaitBinding
import app.code.xfp.databinding.RecyclerDeliverBinding
import app.code.xfp.objects.Order
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class WorkAdapter(private val list: ArrayList<Order>) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private lateinit var mlistener : onItemClickListener
    private var data = FirebaseData()
    val ITEM_DELIVERED = 2
    val ITEM_AWAIT = 1

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_DELIVERED){
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_deliver, parent,false)
            deliveredViewHolder(itemView, mlistener)
        }
        else{
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_await, parent,false)
            viewHolder(itemView, mlistener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentitem = list[position]

        if (holder.javaClass == WorkAdapter.viewHolder::class.java){
            val viewHolder = holder as viewHolder
            viewHolder.binding.titleOrder.text = currentitem.offerTitle

            if (currentitem.orderDate != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.orderDate.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val time = "$day.$month.$year"
                viewHolder.binding.deliveryDate.text = time
            }

            val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.customerID}.png")
            var localfile = File.createTempFile("tempImage2","png")
            storageReference.getFile(localfile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                viewHolder.binding.profileIcon.setImageBitmap(bitmap)
            }

            //Getting information from database and putting showing it
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.username.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.customerID).addListenerForSingleValueEvent(listener)

            val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.sellerID}.png")
            var localFile = File.createTempFile("tempImage","png")
            storageReference2.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                viewHolder.binding.profileIcon2.setImageBitmap(bitmap)
            }

            //Getting information from database and putting showing it
            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.usernameSeller.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.sellerID).addListenerForSingleValueEvent(listener2)

        }
        else{
            val viewHolder = holder as deliveredViewHolder
            viewHolder.binding.titleOrder.text = currentitem.offerTitle

            if (currentitem.deliveryDate != ""){
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentitem.deliveryDate.toLong()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                val time = "$day.$month.$year"
                viewHolder.binding.deliveryDate.text = time
            }

            val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.customerID}.png")
            var localfile = File.createTempFile("tempImage2","png")
            storageReference.getFile(localfile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                viewHolder.binding.profileIcon.setImageBitmap(bitmap)
            }

            //Getting information from database and putting showing it
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.username.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.customerID).addListenerForSingleValueEvent(listener)

            val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.sellerID}.png")
            var localFile = File.createTempFile("tempImage","png")
            storageReference2.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                viewHolder.binding.profileIcon2.setImageBitmap(bitmap)
            }

            //Getting information from database and putting showing it
            val listener2 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (dataP in dataSnapshot.children) {
                        val user = dataP.getValue(User::class.java)
                        viewHolder.binding.usernameSeller.text = user!!.name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            }
            data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.sellerID).addListenerForSingleValueEvent(listener2)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val listItem = list[position]

        return if (listItem.status == "delivered"){
            ITEM_DELIVERED
        }
        else{
            ITEM_AWAIT
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class deliveredViewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        var binding: RecyclerDeliverBinding = RecyclerDeliverBinding.bind(itemView)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
    inner class viewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        var binding: RecyclerAwaitBinding = RecyclerAwaitBinding.bind(itemView)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}