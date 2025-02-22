package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ChatRoomAdapter
import app.code.xfp.adapters.MessageAdapter
import app.code.xfp.databinding.ChatRoomBinding
import app.code.xfp.objects.Message
import app.code.xfp.objects.TransactionClass
import app.code.xfp.objects.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatRoom : AppCompatActivity() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var data = FirebaseData()
    private lateinit var binding: ChatRoomBinding

    private var messages : ArrayList<Message> = ArrayList()
    private var senderRoom : String = ""
    private var receiverRoom : String = ""
    private var senderUid : String = ""
    private var receiverUid : String = ""
    private var isBanned : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkInternet()
        checkUser()

        binding = ChatRoomBinding.inflate(layoutInflater)



        //Getting information's from intent and Firebase storage
        senderUid = intent.getStringExtra("senderUid").toString()
        receiverUid= intent.getStringExtra("receiverUid").toString()


        if (receiverUid == auth.currentUser!!.uid) {
            val temp = receiverUid
            receiverUid = senderUid
            senderUid = temp
        }

        val listener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    binding.username.text = user!!.name
                    if (user.ballance.toString() == "-1") isBanned = true
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("id").equalTo(receiverUid).addListenerForSingleValueEvent(listener2)


        val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${receiverUid}.png")
        var localfile = File.createTempFile("tempImage","png")
        storageReference.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.profileIcon.setImageBitmap(bitmap)
        }

        var adapter : MessageAdapter = MessageAdapter(this,messages)
        binding.recyclerChatRoom.adapter = adapter
        binding.recyclerChatRoom.layoutManager = LinearLayoutManager(this)

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        //Adding text message to database
        binding.messageSend.setOnClickListener{
            if (!isBanned){
                val textMsg = binding.messageBox.text.toString().trim()
                val messageObj = Message(textMsg,senderUid)

                if  (messageObj.message != ""){
                    data.getDatabaseReferenceChatRoom()!!.child(senderRoom).child("messages")
                        .push().setValue(messageObj).addOnSuccessListener {
                            data.getDatabaseReferenceChatRoom()!!.child(receiverRoom).child("messages")
                                .push().setValue(messageObj)
                        }
                    binding.messageBox.setText("")
                }
                else binding.messageBox.setText("")
            }
            else{
                StyleableToast.makeText(this, "Cannot send message. User is banned!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        }

        //Getting chats from database
        data.getDatabaseReferenceChatRoom()!!.child(senderRoom).child("messages").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()

                for (snapshot1 in snapshot.children){
                    val messageVal : Message = snapshot1.getValue(Message::class.java)!!
                    messages.add(messageVal)
                }
                adapter.notifyDataSetChanged()
                binding.recyclerChatRoom.scrollToPosition(messages.size -1)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        binding.customerInfoC.setOnClickListener{
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("name",binding.username.text)
            intent.putExtra("sellerId",receiverUid)
            startActivity(intent)
        }


        setContentView(binding.root)
    }


    private fun getCurrentDate(): String{
        return  System.currentTimeMillis().toString()
    }

    private fun checkDelete(){
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    if (user!!.name == "DELETED USER" && user.ballance == -1){
                        val intent = Intent(applicationContext, NoAccount::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener)
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
        else{
            checkDelete()
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