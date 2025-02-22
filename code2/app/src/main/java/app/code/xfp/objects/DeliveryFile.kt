package app.code.xfp.objects

class DeliveryFile {
    var path : String = ""
    var id : String = ""
    var uploaded : String = ""
    var type : String = ""

    constructor(path: String,id : String, uploaded: String, type : String) {
        this.path = path
        this.uploaded = uploaded
        this.id = id
        this.type = type
    }
    constructor(){}
}