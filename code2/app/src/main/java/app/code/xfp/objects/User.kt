package app.code.xfp.objects

class User {
    var name = ""
    var email = ""
    var phoneNumber = ""
    var id = ""
    var ballance = 1000
    var countryCode = ""
    var country = ""
    var date = ""
    var rating = ""
    var description = ""
    var joinDate = ""

    constructor(
        name: String,
        email: String,
        phoneNumber: String,
        ballance: Int,
        countryCode: String,
        country: String,
        date: String,
        description: String,
        joinDate: String
    ) {
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
        this.id = "0"
        this.ballance = ballance
        this.countryCode = countryCode
        this.country = country
        this.date = date
        this.description = description
        this.joinDate = joinDate
    }

    constructor(){
        this.name = ""
        this.email = ""
        this.phoneNumber = ""
        this.id = ""
        this.ballance = -1
        this.countryCode = ""
        this.country = ""
        this.date = ""
        this.rating = ""
        this.description = ""
        this.joinDate = ""
    }
}