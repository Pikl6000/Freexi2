package app.code.xfp.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.Offer
import app.code.xfp.objects.Review
import app.code.xfp.objects.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File

class ViewAdapter(private val userList : ArrayList<Offer>) : RecyclerView.Adapter<ViewAdapter.MyViewHolder>() {
    private lateinit var mlistener : onItemClickListener
    private var data = FirebaseData()
    private val VIEW_TYPE_ITEM = 0
    private val VIEW_TYPE_LOADING = 1
    private var isLoading = false

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent,false)

        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = userList[position]

        val desc = Html.fromHtml(currentitem.smallDescription)

        holder.description.text = desc
        holder.price.text = currentitem.price.toString()
        holder.title.text = currentitem.title

        val storageReference = FirebaseStorage.getInstance().reference.child("offer_images/"+currentitem.pathImage)
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.image.setImageBitmap(bitmap)
        }
        val storageReference2 = FirebaseStorage.getInstance().reference.child("profile_images/${currentitem.sellerId}.png")
        var localfile2 = File.createTempFile("tempImage2","png")
        storageReference2.getFile(localfile2).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile2.absolutePath)
            holder.userImage.setImageBitmap(bitmap)
        }
        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    holder.creator.text = user!!.name
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(currentitem.sellerId).addListenerForSingleValueEvent(listener)

        var counter :Int = 0
        var sum :Float = 0F
        val listener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val review = dataP.getValue(Review::class.java)
                    sum += review!!.rating
                    counter++
                }
                holder.rating.rating = sum/counter
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceReview()!!.orderByChild("toId").equalTo(currentitem.id).addValueEventListener(listener2)
    }

    override fun getItemCount(): Int {
        return userList.size
    }



    class MyViewHolder(itemView : View,listener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val description : TextView = itemView.findViewById(R.id.description)
        val image : ImageView = itemView.findViewById(R.id.imageView)
        val rating : RatingBar = itemView.findViewById(R.id.ratingBar)
        val creator : TextView = itemView.findViewById(R.id.username)
        val price : TextView = itemView.findViewById(R.id.offerPrice)
        val userImage : AvatarView = itemView.findViewById(R.id.profileIcon)
        val title : TextView = itemView.findViewById(R.id.offerTitle)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

        }
    }
}