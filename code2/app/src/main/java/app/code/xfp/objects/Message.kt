package app.code.xfp.objects

class Message {
    var message = ""
    var senderId = ""

    constructor(){}
    constructor(
        message: String,
        senderId: String, )
    {
        this.message = message
        this.senderId = senderId
    }

}