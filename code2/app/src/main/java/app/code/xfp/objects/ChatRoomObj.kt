package app.code.xfp.objects

class ChatRoomObj {
    var senderUid : String = ""
    var receiverUid : String = ""
    var id : String = ""

    constructor(){}
    constructor(senderUid: String, receiverUid: String) {
        this.senderUid = senderUid
        this.receiverUid = receiverUid
        this.id = receiverUid+senderUid
    }
}