package app.code.xfp.adapters

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.R
import app.code.xfp.databinding.ReceiveMsgBinding
import app.code.xfp.databinding.SendMsgBinding
import app.code.xfp.objects.Message
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(context : Context,val messageList : ArrayList<Message>):RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    val ITEM_SENT = 2
    val ITEM_RECEIVE = 1
    var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT){
            val view = LayoutInflater.from(context).inflate(R.layout.send_msg,parent,false)
            sendViewHolder(view)
        }
        else{
            val view = LayoutInflater.from(context).inflate(R.layout.receive_msg,parent,false)
            receiveViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
       val messages = messageList[position]

        return if (FirebaseAuth.getInstance().uid == messages.senderId){
            ITEM_SENT
        }
        else{
            ITEM_RECEIVE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder.javaClass == sendViewHolder::class.java){
            val viewHolder = holder as sendViewHolder
            viewHolder.binding.messageSend.text = message.message
        }
        else {
            val viewHolder = holder as receiveViewHolder
            viewHolder.binding.messageReceive.text = message.message
        }
    }

    override fun getItemCount(): Int = messageList.size



    inner class sendViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var binding:SendMsgBinding = SendMsgBinding.bind(itemView)
    }
    inner class receiveViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var binding:ReceiveMsgBinding = ReceiveMsgBinding.bind(itemView)
    }
    init {
        this.context = context
    }

}