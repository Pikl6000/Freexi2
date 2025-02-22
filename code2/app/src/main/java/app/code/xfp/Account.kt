package app.code.xfp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.code.xfp.admin.AdminPanel
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import io.getstream.avatarview.AvatarView
import io.getstream.avatarview.coil.loadImage
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File


class Account : Fragment(), View.OnClickListener {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    internal var userLogg: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val view: View = inflater.inflate(app.code.xfp.R.layout.fragment_account,container,false)

        //Declaring variables in layout
        val textName: TextView = view.findViewById(R.id.userName)
        val textBall: TextView = view.findViewById(R.id.userBall)

        val editPersonalInfo : Button = view.findViewById(R.id.editAccountButton)
        val editPassword : Button = view.findViewById(R.id.editAccountPassword)
        val logout : Button = view.findViewById(R.id.editAccountLogout)
        val profilePic : AvatarView = view.findViewById(R.id.profileIcon)
        val createoffer : Button = view.findViewById(R.id.createOrder)
        val manageoffers : Button = view.findViewById(R.id.editMyOrders)
        val editpicture : Button = view.findViewById(R.id.editAccountImage)
        val wallet : Button = view.findViewById(R.id.walletButton)
        val chat : Button = view.findViewById(R.id.chats)
        val orders : Button = view.findViewById(R.id.deliveries)
        val admin : Button = view.findViewById(R.id.adminButton)
        val notify : Button = view.findViewById(R.id.notifications)
        val contact : Button = view.findViewById(R.id.contactSupport)

        editPersonalInfo.setOnClickListener(this)
        editPassword.setOnClickListener(this)
        logout.setOnClickListener(this)
        createoffer.setOnClickListener(this)
        manageoffers.setOnClickListener(this)
        editpicture.setOnClickListener(this)
        wallet.setOnClickListener(this)
        chat.setOnClickListener(this)
        orders.setOnClickListener(this)
        textName.setOnClickListener(this)
        admin.setOnClickListener(this)
        profilePic.setOnClickListener(this)
        notify.setOnClickListener(this)
        contact.setOnClickListener(this)

        //Getting information from database and putting showing it
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    textName.text = user!!.name
                    textBall.text = user.ballance.toString()

                    val storageReference = FirebaseStorage.getInstance().reference.child("profile_images/${user.id}.png")
                    var localfile = File.createTempFile("tempImage","png")
                    storageReference.getFile(localfile).addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                        profilePic.loadImage(bitmap)
                    }.addOnFailureListener{
                        profilePic.loadImage(R.drawable.person)
                    }

                    if (auth.currentUser?.uid == "TfwGoSbrbqV4cW55tCys9qLKR8d2"){
                        admin.visibility = View.VISIBLE
                    }
                    data.getDatabaseReference()!!.removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(user!!.email).addListenerForSingleValueEvent(listener)

        return view
    }

    companion object {
        fun newInstance(): Home {
            return Home()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.editAccountButton ->{
                activity?.let{
                    val intent = Intent (it, AccountInformation::class.java)
                    it.startActivity(intent)
                    //it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.editAccountPassword ->{
                activity?.let{
                    val intent = Intent (it, AccountPassword::class.java)
                    it.startActivity(intent)
                    //it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.editAccountLogout ->{
                auth.signOut()
                val applicationContext = requireActivity().getApplicationContext()
                StyleableToast.makeText(applicationContext, "Logged out!", Toast.LENGTH_SHORT, R.style.myToastDone).show()
                activity?.let{
                    val intent = Intent (it, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                }
            }
            R.id.createOrder ->{
                activity?.let{
                    val intent = Intent (it, CreateOffer::class.java)
                    it.startActivity(intent)
                   // it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.editMyOrders ->{
                activity?.let{
                    val intent = Intent (it, ManageOffers::class.java)
                    it.startActivity(intent)
                    //it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.editAccountImage ->{
                activity?.let{
                    val intent = Intent (it, AccountPicture::class.java)
                    it.startActivity(intent)
                   // it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.walletButton ->{
                activity?.let{
                    val intent = Intent (it, Wallet::class.java)
                    it.startActivity(intent)
                   // it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.chats ->{
                activity?.let{
                    val intent = Intent (it, ChatSelection::class.java)
                    it.startActivity(intent)
                    //it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.deliveries ->{
                activity?.let{
                    val intent = Intent (it, Deliveries::class.java)
                    it.startActivity(intent)
                    //it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.userName ->{
                activity?.let{
                    val intent = Intent(it,UserProfile::class.java)
                    intent.putExtra("sellerId",auth.currentUser!!.uid)
                    startActivity(intent)
                   // it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.profileIcon ->{
                activity?.let{
                    val intent = Intent(it,UserProfile::class.java)
                    intent.putExtra("sellerId",auth.currentUser!!.uid)
                    startActivity(intent)
                }
            }
            R.id.adminButton ->{
                activity?.let{
                    val intent = Intent (it, AdminPanel::class.java)
                    it.startActivity(intent)
                   // it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.notifications ->{
                activity?.let{
                    val intent = Intent (it, Notifications::class.java)
                    it.startActivity(intent)
                  //  it.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            R.id.contactSupport ->{
                activity?.let{
                    val intent = Intent (it, Support::class.java)
                    it.startActivity(intent)
                }
            }
            else -> {
                val applicationContext = requireActivity().getApplicationContext()
                StyleableToast.makeText(applicationContext, "Well, idk error!", Toast.LENGTH_SHORT, R.style.myToastFail).show()
            }
        }
    }
}
