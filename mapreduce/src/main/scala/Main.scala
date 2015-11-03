import reactivemongo.core.commands.RawCommand

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import reactivemongo.api._
import reactivemongo.bson.{BSONString, BSONDocument}
import reactivemongo.api.collections.bson.BSONCollection

object Main {

  def mapReduce(db: DefaultDB) = {
    val mapReduceCommand = BSONDocument(
      "mapreduce" -> "sessions",
      "map" -> BSONString("function(){emit(this.poleid,this)}"), // TODO: add javascript if to check if it is in certain time frame
      "reduce" -> BSONString("function(key, values){var k={}; k.kwh = 0; k.price = 0; values.forEach(function(item){k.kwh += item.kwh; k.price += item.price}); return k;}"),
      "out" -> BSONDocument("replace" -> "sessionsout") // TODO: The out collection (to be displayed somewhere?)
    )
    val result = db.command(RawCommand(mapReduceCommand))
  }

  def listDocs(collection: BSONCollection) = {
    // Select only the documents which field 'firstName' equals 'Jack'
    val query = BSONDocument("poleid" -> "P0001")
    // select only the fields 'lastName' and '_id'
    val filter = BSONDocument(
      "poleid" -> 1,
      "startDate" -> 1,
      "endDate" -> 1,
      "kwh" -> 1,
      "price" -> 1)

    // Or, the same with getting a list
    val futureList: Future[List[BSONDocument]] =
      collection.
        find(query, filter).
        cursor[BSONDocument].
        collect[List]()

    futureList.map { list =>
      list.foreach { doc =>
        println(s"found document: ${BSONDocument pretty doc}")
      }
    }
  }

  def main(args: Array[String]) {
    // gets an instance of the driver
    // (creates an actor system)
    val driver = new MongoDriver
    // TODO: read these values from etcd!
    val connection = driver.connection(List("mongodb1","mongodb2","mongodb3"))

    // Gets a reference to the database "plugin"
    val db = connection("wacc")

    // Gets a reference to the collection "acoll"
    // By default, you get a BSONCollection.
    val collection = db("sessions")

    // List all sessions
    //listDocs(collection)

    mapReduce(db)

    // Be able to exit the application
    driver.close()

  }
}
