package app.code.xfp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.code.xfp.adapters.ViewAdapter
import app.code.xfp.databinding.FragmentHomeBinding
import app.code.xfp.objects.Offer
import app.code.xfp.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class Home : Fragment(), View.OnClickListener{
    private lateinit var auth: FirebaseAuth
    private var data = FirebaseData()
    private var uid:String = ""
    private var binding: FragmentHomeBinding? = null

    private var offerList = ArrayList<Offer>()
    private var showList = ArrayList<Offer>()
    private var adapter : ViewAdapter = ViewAdapter(showList)
    private var maxSearch : Int = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding!!.dataoutput.adapter = adapter
        binding!!.dataoutput.layoutManager = LinearLayoutManager(requireContext())
        binding!!.dataoutput.setHasFixedSize(true)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataP in dataSnapshot.children) {
                    val user = dataP.getValue(User::class.java)
                    uid =  user!!.id
                    binding!!.welcomeTextT.text = user.name
                    data.getDatabaseReference()!!.removeEventListener(this)
                    getData(3)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        data.getDatabaseReference()!!.orderByChild("email").equalTo(auth.currentUser?.email).addListenerForSingleValueEvent(listener)

        adapter.setOnItemClickListener(object : ViewAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                val applicationContext = requireActivity().getApplicationContext()
                val intent = Intent(applicationContext,OfferDetail::class.java)
                val offer = offerList.get(position)
                intent.putExtra("title",offer.title)
                intent.putExtra("image",offer.pathImage)
                intent.putExtra("id",offer.id)
                intent.putExtra("rating",offer.rating)
                intent.putExtra("description",offer.description)
                intent.putExtra("descriptionS",offer.smallDescription)
                intent.putExtra("price",offer.price.toString())
                intent.putExtra("userId",offer.sellerId)
                startActivity(intent)
            }
        })

        binding!!.searchImageCon.setOnClickListener {
            if (binding!!.searchView.visibility == View.VISIBLE) {
                binding!!.searchView.visibility = View.GONE
            } else {
                binding!!.searchView.visibility = View.VISIBLE
                binding!!.searchView.requestFocus()
            }
        }


        binding!!.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchOffers(query.trim(),maxSearch)
                checkOffers()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchOffers(newText.trim(),maxSearch)
                return false
            }
        })

        binding!!.dataoutput.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Check if the user has reached the end of the list
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val isAtEnd = visibleItemCount + firstVisibleItemPosition >= totalItemCount

                if (isAtEnd) {
                    loadNextChunkOfData()
                }
            }
        })
    }

    private fun loadNextChunkOfData(){
        maxSearch += 3
        val newItems = ArrayList<Offer>()
        var new : Boolean = false

        if (showList.size > offerList.size){
            showList.clear()
        }

        for (offer in offerList) {
            if (!showList.contains(offer)) {
                if (newItems.size >= maxSearch - showList.size) {
                    break
                }
                else{
                    new = true
                    newItems.add(offer)
                }
            }
        }
        if (new){
            showList.addAll(newItems)
            maxSearch = showList.size
            adapter.notifyDataSetChanged()
        }
        else{
            maxSearch -= 3
        }
    }


    private fun searchOffers(query: String, max: Int) {
        val searchQuery = data.getDatabaseReferenceOffer()!!.orderByChild("title").startAt(query).endAt(query + "\uf8ff")

        searchQuery.addValueEventListener(object : ValueEventListener {
            var counter : Int = 0
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (offerList.isNotEmpty()) offerList.clear()
                for (snapshot in dataSnapshot.children) {
                    val offer = snapshot.getValue(Offer::class.java)
                    if (offer!!.sellerId != auth.currentUser!!.uid && counter < max) {
                        offerList.add(offer)
                        counter++
                    }
                }
                loadNextChunkOfData()
                checkOffers()
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }
    private fun checkOffers(){
        if (offerList.isEmpty()){
            binding!!.notFound.visibility = View.VISIBLE
            binding!!.dataoutput.visibility = View.GONE
        }
        else{
            binding!!.notFound.visibility = View.GONE
            binding!!.dataoutput.visibility = View.VISIBLE
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

    private fun getData(max : Int){
        data.getDatabaseReferenceOffer()!!.addValueEventListener(object : ValueEventListener {
            val counter : Int = 0
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    offerList.clear()
                    for (i in snapshot.children) {
                        val offer = i.getValue(Offer::class.java)
                        if  (offer!!.sellerId != auth.currentUser!!.uid && counter < max) {
                            offerList.add(offer)
                        }
                    }
                    if (offerList.isEmpty()){
                        binding!!.dataoutput.visibility = View.GONE
                        binding!!.notFound.visibility = View.VISIBLE
                        data.getDatabaseReferenceOffer()!!.removeEventListener(this)
                    }
                    else{
                        loadNextChunkOfData()
                        data.getDatabaseReferenceOffer()!!.removeEventListener(this)
                    }
                } else {
                    binding!!.dataoutput.visibility = View.GONE
                    binding!!.notFound.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

