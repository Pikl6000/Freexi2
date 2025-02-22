package app.code.xfp.adapters

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.ChatRoomObj
import app.code.xfp.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import io.getstream.avatarview.coil.loadImage
import java.io.File

class ChatRoomAdapter(private val chatList : ArrayList<ChatRoomObj>) : RecyclerView.Adapter<ChatRoomAdapter.MyViewHolder>() {
    private lateinit var mlistener : ChatRoomAdapter.onItemClickListener
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
        val currentitem = chatList[position]

        val uid = currentitem.receiverUid

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    holder.user.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.receiverUid).addListenerForSingleValueEvent(listener)

        val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.receiverUid}.png")
        var localfile2 = File.createTempFile("tempImage2", "png")
        storageReference2.getFile(localfile2).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
            holder.image.setImageBitmap(bitmap)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }


    class MyViewHolder(itemView : View, listener: ChatRoomAdapter.onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val user : TextView = itemView.findViewById(R.id.username)
        val image : AvatarView = itemView.findViewById(R.id.profileIcon)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}