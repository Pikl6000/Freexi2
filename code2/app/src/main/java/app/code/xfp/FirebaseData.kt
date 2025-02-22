package app.code.xfp

import app.code.xfp.objects.*
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*

class FirebaseData {
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance("https://xfp-project-15652-default-rtdb.europe-west1.firebasedatabase.app/")
    private var databaseReference: DatabaseReference? = db.getReference("users")
    private var databaseReferenceOffer: DatabaseReference? = db.getReference("offers")
    private var databaseReferenceOrder: DatabaseReference? = db.getReference("orders")
    private var databaseReferenceReview: DatabaseReference? = db.getReference("reviews")
    private var databaseReferenceChatRoom: DatabaseReference? = db.getReference("chat-room")
    private var databaseReferenceTransaction: DatabaseReference? = db.getReference("transactions")
    private var databaseReferenceReport: DatabaseReference? = db.getReference("reports")
    private var databaseReferenceNotification: DatabaseReference? = db.getReference("notifications")


    //Database - User
    fun getDatabaseReference(): DatabaseReference? {
        return databaseReference
    }
    fun add(p: User): Task<Void?> {
        return databaseReference!!.child(p.id).setValue(p)
    }
    fun update(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReference!!.child(key).updateChildren(hashMap)
    }


    //Database - Offer
    fun getDatabaseReferenceOffer(): DatabaseReference? {
        return databaseReferenceOffer
    }
    fun addOffer(o : Offer):Task<Void?>{
        return databaseReferenceOffer!!.child(o.id).setValue(o)
    }
    fun updateOffer(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceOffer!!.child(key).updateChildren(hashMap)
    }


    //Database - Review
    fun getDatabaseReferenceReview(): DatabaseReference? {
        return databaseReferenceReview
    }
    fun addReview(o : Review):Task<Void?>{
        return databaseReferenceReview!!.child(o.id).setValue(o)
    }
    fun updateReview(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceReview!!.child(key).updateChildren(hashMap)
    }


    //Database - Order
    fun getDatabaseReferenceOrder(): DatabaseReference? {
        return databaseReferenceOrder
    }
    fun addOrder(o : Order):Task<Void?>{
        return databaseReferenceOrder!!.child(o.id).setValue(o)
    }
    fun updateOrder(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceOrder!!.child(key).updateChildren(hashMap)
    }


    //Database - Chat Rooms
    fun getDatabaseReferenceChatRoom(): DatabaseReference? {
        return databaseReferenceChatRoom
    }
    fun addChatRoom(o : ChatRoomObj):Task<Void?>{
        return databaseReferenceChatRoom!!.child(o.id).setValue(o)
    }
    fun updateChatRoom(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceChatRoom!!.child(key).updateChildren(hashMap)
    }

    //Database - Transaction
    fun getDatabaseReferenceTransaction(): DatabaseReference? {
        return databaseReferenceTransaction
    }
    fun addTransaction(o : TransactionClass):Task<Void?>{
        return databaseReferenceTransaction!!.child(o.id).setValue(o)
    }
    fun updateTransaction(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceTransaction!!.child(key).updateChildren(hashMap)
    }

    //Database - Reports
    fun getDatabaseReferenceReport(): DatabaseReference? {
        return databaseReferenceReport
    }
    fun addReport(o : Report):Task<Void?>{
        return databaseReferenceReport!!.child(o.id).setValue(o)
    }
    fun updateReport(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceReport!!.child(key).updateChildren(hashMap)
    }

    //Database - Reports
    fun getDatabaseReferenceNotification(): DatabaseReference? {
        return databaseReferenceNotification
    }
    fun addNotification(o : NotificationObj):Task<Void?>{
        return databaseReferenceNotification!!.child(o.id).setValue(o)
    }
    fun updateNotification(key: String, hashMap: HashMap<String, Any>): Task<Void> {
        return databaseReferenceNotification!!.child(key).updateChildren(hashMap)
    }
}