package app.code.xfp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.FirebaseData
import app.code.xfp.R
import app.code.xfp.objects.DeliveryFile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File

class DeliveryAdapter(private val list : ArrayList<DeliveryFile>) : RecyclerView.Adapter<DeliveryAdapter.MyViewHolder>() {
    private lateinit var mlistener : DeliveryAdapter.onItemClickListener
    private var data = FirebaseData()

    interface onItemClickListener {
        fun onItemClick(position: Int)
        fun onLongClick(position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener){
        this.mlistener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_download, parent,false)

        return MyViewHolder(itemView,mlistener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = list[position]

        holder.file.text = "File " + (position + 1 )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(itemView : View, listener: DeliveryAdapter.onItemClickListener) : RecyclerView.ViewHolder(itemView){
        val file : TextView = itemView.findViewById(R.id.fileName)

        init{

            itemView.setOnClickListener {

                listener.onItemClick(adapterPosition)

            }

            itemView.setOnLongClickListener{
                listener.onLongClick(adapterPosition)
                true
            }
        }
    }
}