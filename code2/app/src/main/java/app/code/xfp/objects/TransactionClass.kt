package app.code.xfp.objects

class TransactionClass {
    var from = ""
    var to = ""
    var price = ""
    var orderID = ""
    var date = ""
    var id = ""
    var delivered : Boolean = false

    constructor(from: String, to: String, price: String, orderID: String,date : String,id : String) {
        this.from = from
        this.to = to
        this.price = price
        this.orderID = orderID
        this.date = date
        this.id = id
        this.delivered = false
    }
    constructor() {}
    constructor(from: String, to: String, price: String, orderID: String,date : String,id : String,delivered : Boolean) {
        this.from = from
        this.to = to
        this.price = price
        this.orderID = orderID
        this.date = date
        this.id = id
        this.delivered = delivered
    }
}