package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ChatRoomAdapter
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.ChatSelectionBinding
import app.code.xfp.databinding.OfferDetailBinding
import app.code.xfp.objects.ChatRoomObj
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File

class ChatSelection : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private lateinit var binding: ChatSelectionBinding

    private var chatList : ArrayList<ChatRoomObj> = ArrayList()
    private var adapter = ChatRoomAdapter(chatList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        auth = FirebaseAuth.getInstance()
        checkUser()

        binding = ChatSelectionBinding.inflate(layoutInflater)

        binding.recyclerChat.adapter = adapter
        binding.recyclerChat.layoutManager = LinearLayoutManager(this)

       //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val chatRoom = snapshot.getValue(ChatRoomObj::class.java)

                    val sub : String = chatRoom!!.id.substring(28, 56)
                    val sub2 : String = chatRoom.id.substring(0, 28)
                    if (sub == auth.currentUser!!.uid){
                        chatList.add(chatRoom)
                    }
                    adapter.notifyDataSetChanged()
                    adapter.setOnItemClickListener(object : ChatRoomAdapter.onItemClickListener{
                        override fun onItemClick(position: Int) {
                            val intent = Intent(applicationContext,ChatRoom::class.java)
                            val chatRoom = chatList.get(position)
                            intent.putExtra("receiverUid",chatRoom.receiverUid)
                            intent.putExtra("senderUid",chatRoom.senderUid)
                            startActivity(intent)
                        }
                    })
                }
                checkCon()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReferenceChatRoom()!!.orderByChild("id").addListenerForSingleValueEvent(listener)



        setContentView(binding.root)
    }

    private fun checkCon(){
        if (chatList.isEmpty()){
            binding.recyclerChat.visibility = View.GONE
            binding.bottomH.visibility = View.VISIBLE
        }
        if (chatList.isNotEmpty() && binding.recyclerChat.visibility == View.GONE){
            binding.recyclerChat.visibility = View.VISIBLE
            binding.bottomH.visibility = View.GONE
        }
    }

    private fun checkInternet() {
        if (!isNetworkAvailable()) {
            StyleableToast.makeText(this, "No Connection!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            val intent = Intent(this, NoConnection::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }


    //Check if user is logged in
    private fun checkUser(){
        checkInternet()
        val user = auth.currentUser

        if (user == null){ //If he's not (switch to login activity)
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    override fun onStart() {
        super.onStart()
        checkUser()
    }

    override fun onResume() {
        super.onResume()
        checkUser()
    }

    override fun onRestart() {
        super.onRestart()
        checkUser()
    }
}