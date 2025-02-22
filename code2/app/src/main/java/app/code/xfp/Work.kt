package app.code.xfp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.code.xfp.adapters.ConfirmAdapter
import app.code.xfp.adapters.WorkAdapter
import app.code.xfp.databinding.FragmentWorkBinding
import app.code.xfp.objects.Order
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList


class Work : Fragment(), View.OnClickListener {
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private var binding: FragmentWorkBinding? = null

    private val progressList = ArrayList<Order>()
    private val waitList = ArrayList<Order>()
    private var move : Boolean = false
    private var initialSet : Boolean = true
    private var uid : String = ""
    private var adapter : ConfirmAdapter = ConfirmAdapter(waitList)
    private var adapter2 : WorkAdapter = WorkAdapter(progressList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        auth = FirebaseAuth.getInstance()
        binding = FragmentWorkBinding.inflate(inflater, container, false)

        waitList.clear()
        progressList.clear()

        adapter = ConfirmAdapter(waitList)
        adapter2 = WorkAdapter(progressList)

        binding!!.recyclerWait.adapter = adapter
        binding!!.recyclerWait.layoutManager = LinearLayoutManager(requireContext())

        binding!!.recyclerWork.adapter = adapter2
        binding!!.recyclerWork.layoutManager = LinearLayoutManager(requireContext())

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    var user = dataP.getValue(User::class.java)
                    uid =  user!!.id
                    getData()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser!!.email).addListenerForSingleValueEvent(listener)

        checkEmpty(1)

        binding!!.wait.setOnClickListener{
            if (!move){
                move = true
                checkEmpty(1)
            }
        }
        binding!!.prog.setOnClickListener{
            if (move){
                move = false
                checkEmpty(2)
            }
        }
        return binding!!.root
    }

    private fun getData(){
        data.getDatabaseReferenceOrder()!!.orderByChild("sellerID").equalTo(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    progressList.clear()
                    waitList.clear()
                    for (i in snapshot.children) {
                        val order = i.getValue(Order::class.java)
                        if (order!!.accepted) {
                            if (order.status == "awaiting") progressList.add(order)
                        }
                        else waitList.add(order)
                    }

                    adapter.notifyDataSetChanged()
                    adapter.setOnItemClickListener(object : ConfirmAdapter.onItemClickListener{
                        override fun onItemClick(position: Int) {
                            val applicationContext = requireActivity().applicationContext
                            val intent = Intent(applicationContext,AcceptOrder::class.java)
                            val order = waitList.get(position)
                            intent.putExtra("orderId",order.id)
                            startActivity(intent)
                        }
                    })
                    adapter2.notifyDataSetChanged()
                    adapter2.setOnItemClickListener(object : WorkAdapter.onItemClickListener{
                        override fun onItemClick(position: Int) {
                            val applicationContext = requireActivity().applicationContext
                            val intent = Intent(applicationContext,DeliverOrder::class.java)
                            val order = progressList.get(position)
                            intent.putExtra("orderId",order.id)
                            startActivity(intent)
                        }
                    })

                    if (initialSet){
                        if (adapter2.itemCount > 0){
                            binding!!.work.visibility = View.VISIBLE
                            binding!!.bottomH.visibility = View.GONE
                            binding!!.recyclerWait.visibility = View.GONE
                            binding!!.recyclerWork.visibility = View.VISIBLE
                        }
                        initialSet = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkEmpty(x : Int){
        if (x == 1){
            if (adapter.itemCount > 0 && move){
                binding!!.work.visibility = View.VISIBLE
                binding!!.bottomH.visibility = View.GONE
                binding!!.recyclerWork.visibility = View.GONE
                binding!!.recyclerWait.visibility = View.VISIBLE
            }
            else{
                binding!!.work.visibility = View.GONE
                binding!!.bottomH.visibility = View.VISIBLE
            }
        }
        else{
            if (adapter2.itemCount > 0 && !move){
                binding!!.work.visibility = View.VISIBLE
                binding!!.bottomH.visibility = View.GONE
                binding!!.recyclerWait.visibility = View.GONE
                binding!!.recyclerWork.visibility = View.VISIBLE
            }
            else{
                binding!!.work.visibility = View.GONE
                binding!!.bottomH.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun newInstance(): Home {
            return Home()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            else -> {
            }
        }
    }


}