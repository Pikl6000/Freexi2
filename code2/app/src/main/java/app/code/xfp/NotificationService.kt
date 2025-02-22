package app.code.xfp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import app.code.xfp.MainActivity
import app.code.xfp.objects.NotificationObj
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationService : Service() {

    private var data : FirebaseData = FirebaseData()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isServiceRunning = false

    val name = "alert"
    val descriptionText = "Notifications of App"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel("alert", name, importance).apply {
        description = descriptionText
    }
    val channel2 = NotificationChannel("notification", "notification", importance).apply {
        description = descriptionText
    }
    val channel3 = NotificationChannel("order", "order", importance).apply {
        description = descriptionText
    }
    val channel4 = NotificationChannel("report", "report", importance).apply {
        description = descriptionText
    }
    val channel5 = NotificationChannel("warning", "warning", importance).apply {
        description = descriptionText
    }

        override fun onCreate() {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(channel2)
            notificationManager.createNotificationChannel(channel3)
            notificationManager.createNotificationChannel(channel4)
            notificationManager.createNotificationChannel(channel5)
            super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isServiceRunning) {
            return START_STICKY
        }
        isServiceRunning = true
        listenForNotifications()
        return START_STICKY
    }

    private fun listenForNotifications() {
        if (auth.currentUser?.uid != ""){
            data.getDatabaseReferenceNotification()!!.orderByChild("toId").equalTo(auth.currentUser!!.uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (childSnapshot in snapshot.children) {
                        val notification = childSnapshot.getValue(NotificationObj::class.java)
                        if (notification?.seen == false) {
                            sendNotification(notification)
                            markNotificationAsSeen(notification)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun sendNotification(notification: NotificationObj?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, notification!!.channel)
            .setSmallIcon(R.drawable.logo)
            .setColorized(true)
            .setColor(resources.getColor(R.color.dark_blue))
            .setContentTitle(notification?.title)
            .setContentText(notification?.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        builder.setDefaults(Notification.DEFAULT_SOUND)
        notificationManager.notify(1, builder.build())
    }


    private fun markNotificationAsSeen(notification: NotificationObj?) {
        // mark the notification as seen in the database
        val notificationRef = data.getDatabaseReferenceNotification()!!.child(notification?.id.toString())
        val hashMap = HashMap<String, Any>()
        hashMap.put("seen",true)
        notificationRef.updateChildren(hashMap)
    }

    public fun getId():String{
        return auth.currentUser!!.uid
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}