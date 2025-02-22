package app.code.xfp.adapters

import android.graphics.BitmapFactory
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import io.getstream.avatarview.coil.loadImage
import java.io.File

class ConfirmAdapter(private val userList : ArrayList<Order>) : RecyclerView.Adapter<ConfirmAdapter.MyViewHolder>() {
    private lateinit var mlistener : onItemClickListener
    private var data = FirebaseData()

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_confirm, parent,false)

        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = userList[position]

        val desc = Html.fromHtml(currentitem.description)
        holder.title.text = currentitem.offerTitle
        holder.request.text = desc

        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.customerID}.png")
        var localfile = File.createTempFile("tempImage2","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.image.setImageBitmap(bitmap)
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
    }

    override fun getItemCount(): Int {
        return userList.size
    }


    class MyViewHolder(itemView : View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val title : TextView = itemView.findViewById(R.id.orderTitle)
        val image : AvatarView = itemView.findViewById(R.id.profileIcon)
        val customer : TextView = itemView.findViewById(R.id.username)
        val request : TextView = itemView.findViewById(R.id.requiments)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}