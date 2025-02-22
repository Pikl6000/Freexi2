package app.code.xfp.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.databinding.ReceiveMsgBinding
import app.code.xfp.databinding.RecyclerChatBinding
import app.code.xfp.databinding.SendMsgBinding
import app.code.xfp.objects.Message
import app.code.xfp.objects.Offer
import app.code.xfp.objects.Order
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class UserAdapter(private val list : ArrayList<User>) : RecyclerView.Adapter<UserAdapter.MyViewHolder>() {
    private lateinit var mlistener : onItemClickListener
    private var data = FirebaseData()

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_chat, parent,false)
        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = list[position]

        holder.title.text = currentitem.name

        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.id}.png")
        var localfile = File.createTempFile("tempImage2","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.image.setImageBitmap(bitmap)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView : View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val title : TextView = itemView.findViewById(R.id.username)
        val image : AvatarView = itemView.findViewById(R.id.profileIcon)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}