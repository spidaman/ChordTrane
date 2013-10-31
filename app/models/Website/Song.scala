package models.website

import anorm._
import anorm.RowParser
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.api.Logger


case class Song(id: Int, rawText: String, title: String, composer: String, 
	dateCreated: String, timeSig: Int, currentKey: String, destinationKey: String, transposeOn: Boolean, romanNumeral: Boolean, userId: Int)

object Song extends DatabaseObject{

  //when new song created
  val BlankSong = Song(-1, "", "New Song", "Title", "Composer", 4, "C", "C", false, false, -1)


  val songParser: RowParser[Song] = {
    import anorm.~
    get[Int]("id") ~
    get[String]("rawText") ~
    get[String]("title") ~
    get[String]("composer") ~
    get[String]("dateCreated") ~ 
    get[Int]("timeSig") ~ 
    get[String]("currentKey") ~
    get[String]("destinationKey") ~ 
    get[Boolean]("transposeOn") ~
    get[Boolean]("romanNumeral") ~ 
    get[Int]("userId") map {
    case id ~ rawText ~ title ~ composer ~ dateCreated ~ timeSig ~ currentKey ~ destinationKey ~ transposeOn ~ romanNumeral ~ userId =>
        Song(id, rawText, title, composer, dateCreated, timeSig, currentKey, destinationKey, transposeOn, romanNumeral, userId)
    } 
  }

  def getSong(id: Int) = getSingleWithID[Song](songParser, id, "id", "songs")
  def getSongsByUser(userId: Int) = getAllOfID[Song](songParser, userId, "userId", "songs").map(song => LabelAndId(song.title, song.id))

  def updateSong(song: Song) = DB.withConnection{
    implicit connection=>
    SQL("""
      UPDATE songs
      SET
      rawText = {rawText},
      title = {title},
      composer = {composer},
      dateCreated = {dateCreated},
      timeSig = {timeSig},
      currentKey = {currentKey},
      destinationKey = {destinationKey},
      transposeOn = {transposeOn},
      romanNumeral = {romanNumeral},
      userId = {userId}
      WHERE id = {id}
      """
   ).on(
    "id" -> song.id,
    "rawText" -> song.rawText,
    "title" -> song.title,
    "composer" -> song.composer,
    "dateCreated" -> song.dateCreated,
    "timeSig" -> song.timeSig,
    "currentKey" -> song.currentKey,
    "destinationKey" -> song.destinationKey,
    "transposeOn" -> song.transposeOn,
    "romanNumeral" -> song.romanNumeral,
    "userId" -> song.userId
   ).executeUpdate()
  }

  def songConversion(song: Song) = {
    val parserSong = models.Song(song.rawText,
      song.currentKey, song.destinationKey, song.timeSig,
      song.transposeOn, song.romanNumeral)
    parserSong

  }

  def formatText(song: Song) = {
    models.Master.printSong(songConversion(song))
  }
  def exportMusicXML(song: Song) = {
    val path = s"public/MusicXML/${song.title}.xml" 
    models.Master.exportXML(songConversion(song), path, song.title, song.composer)
    path
  }

  def playback(song: Song) = {
    val path = s"public/MusicXML/${song.title}.mid"
    models.Master.playback(songConversion(song), path)
    path
  }

  def newSong(userId: Int) = DB.withConnection{
    implicit connection => 
    val songId = SQL(
      """
      INSERT INTO songs (rawText, title, composer, dateCreated, timeSig, userId, currentKey, destinationKey, transposeOn, romanNumeral)
      VALUES ({rawText}, {title}, {composer}, {dateCreated}, {timeSig}, {userId}, {currentKey}, {destinationKey}, {transposeOn}, {romanNumeral})
      """).on(
        "rawText" -> BlankSong.rawText,
        "title" -> BlankSong.title,
        "composer" -> BlankSong.composer,
        "dateCreated" -> BlankSong.dateCreated,
        "timeSig" -> BlankSong.timeSig,
        "userId" -> userId,
        "currentKey" -> BlankSong.currentKey,
        "destinationKey" -> BlankSong.destinationKey,
        "transposeOn" -> BlankSong.transposeOn,
        "romanNumeral" -> BlankSong.romanNumeral
        ).executeInsert().get.toInt //returns id of song added

    PlaybackSettings.newPlaybackSettings(songId)

    songId
  }



 
} 