package app.code.xfp.objects

class Offer {
    var title = ""
    var price = 0
    var smallDescription = ""
    var description = ""
    var pathImage = ""
    var sellerId = ""
    var rating : Float = 0.0f
    var id = ""
    var visible = true


    constructor() {
        this.title = ""
        this.price = 0
        this.smallDescription = ""
        this.description = ""
        this.pathImage = ""
        this.sellerId = ""
        this.rating = 0f
        this.id = "-1"
        this.visible = true
    }

    constructor(
        title: String,
        price: Int,
        smallDescription: String,
        description: String,
        pathImage: String,
        sellerId: String,
        rating: Float,
        id: String,
        visible: Boolean
    ) {
        this.title = title
        this.price = price
        this.smallDescription = smallDescription
        this.description = description
        this.pathImage = pathImage
        this.sellerId = sellerId
        this.rating = rating
        this.id = id
        this.visible = visible
    }


}