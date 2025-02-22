package app.code.xfp.objects

import java.util.*

class NotificationObj {
    var fromId : String = ""
    var toId : String = ""
    var channel : String = ""
    var title : String = ""
    var text : String = ""
    var id : String = ""
    var seen : Boolean = false
    var date : String = ""

    constructor(
        fromId: String,
        toId: String,
        channel: String,
        title: String,
        text: String,
    ) {
        this.fromId = fromId
        this.toId = toId
        this.channel = channel
        this.title = title
        this.text = text
        this.date = System.currentTimeMillis().toString()
        this.id = UUID.randomUUID().toString()
    }

    constructor(){}
}