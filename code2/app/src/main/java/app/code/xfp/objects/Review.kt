package app.code.xfp.objects

import java.util.*

class Review {
    var id : String = UUID.randomUUID().toString()
    var rating : Float = 0F
    var fromId : String= ""
    var type : String = ""
    var toId : String = ""
    var orderId : String = ""
    var text : String= ""
    var date : String = System.currentTimeMillis().toString()

    constructor(rating: Float, fromId: String, type: String, toId: String, orderId: String, text: String) {
        this.rating = rating
        this.fromId = fromId
        this.type = type
        this.toId = toId
        this.text = text
        this.orderId = orderId
    }
    constructor(){}
}