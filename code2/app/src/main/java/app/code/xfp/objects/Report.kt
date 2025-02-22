package app.code.xfp.objects

import java.util.*

class Report {
    var type : String = ""
    var date : String = ""
    var reportedId : String = ""
    var message : String = ""
    var userId : String = ""
    var accept : Boolean = false
    var id : String = ""

    constructor(
        type: String,
        date: String,
        reportedId: String,
        message: String,
        userId: String,
        accept: Boolean
    ) {
        this.type = type
        this.date = date
        this.reportedId = reportedId
        this.message = message
        this.userId = userId
        this.accept = accept
        this.id = userId+reportedId
    }

    constructor(
        type: String,
        date: String,
        message: String,
        userId: String,
        accept: Boolean
    ) {
        this.type = type
        this.date = date
        this.message = message
        this.userId = userId
        this.accept = accept
        this.id = UUID.randomUUID().toString()
    }

    constructor(){}

}