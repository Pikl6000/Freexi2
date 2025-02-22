package app.code.xfp.objects

class Order {
    var id = ""
    var sellerID = ""
    var customerID = ""
    var orderDate = ""
    var deliveryDate = ""
    var price = ""
    var accepted = false
    var description = ""
    var offerId = ""
    var offerTitle = ""
    var status = ""
    var transactionId = ""

    constructor(
        id : String,
        sellerID: String,
        customerID: String,
        orderDate: String,
        price: String,
        description: String,
        offerId: String,
        offerTitle: String,
        transactionId : String
    ) {
        this.sellerID = sellerID
        this.customerID = customerID
        this.orderDate = orderDate
        this.price = price
        this.description = description
        this.id = id
        this.offerId = offerId
        this.offerTitle = offerTitle
        this.transactionId = transactionId
        this.status = "awaiting"
    }

    constructor(){}

    constructor(
        id: String,
        sellerID: String,
        customerID: String,
        orderDate: String,
        deliveryDate: String,
        price: String,
        accepted: Boolean,
        description: String,
        offerId: String,
        offerTitle: String,
        status : String,
        transactionId : String
    ) {
        this.id = id
        this.sellerID = sellerID
        this.customerID = customerID
        this.orderDate = orderDate
        this.deliveryDate = deliveryDate
        this.price = price
        this.accepted = accepted
        this.description = description
        this.offerId = offerId
        this.offerTitle = offerTitle
        this.status = status
        this.transactionId = transactionId
    }
}