package controllers

import models._
import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.data.Forms._
import play.api.libs.ws.WS
import org.joda.time._
import java.util.{ Date, Locale }
import java.text._
import play.api.data.validation.Constraints._
import Bid._

import play.api.libs.json._
import play.api.libs.functional.syntax._

import models.Formats._

object Workspace extends Controller {

  /****************************** POST request *****************************************/

  /* ------------------ Actions ------------------ */

  def getBanners = Action(parse.json) { implicit request =>
    {
      val data = request.body
      val user = User.findByName((data \ ("user")).as[String]).get
      val net = (data \ ("net")).as[String]
      val login = (data \ ("_login")).as[String]
      val token = (data \ ("_token")).as[String]
      val id = (data \ ("network_campaign_id")).as[String]

      // get BannersInfo from Yandex
      val (bannerInfo_List, json_banners) = API_yandex.getBanners(login, token, List(id.toInt))

      if (bannerInfo_List.isDefined) {
        println("!!! SUCCESS getBanners !!!")

        // post BannersInfo to BID
        if (API_bid.postBannerReports(user, net, id, json_banners))
          println("!!! ActualBids and NetAdvisedBids is POSTED to BID !!!")
        else
          println("??? ActualBids and NetAdvisedBids is NOT POSTED to BID !!!")

        //return List[BannerInfo] to client browser
        Ok(Json.toJson[List[BannerInfo]](bannerInfo_List.get))
      } else println("??? FAILED getBanners ???"); BadRequest
    }
  }

  def postCampaign = Action { implicit request =>
    {
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val camp = API_bid.postCampaign(
            user = User.findByName((data \ ("user")).as[String]).get,
            net = (data \ ("net")).as[String],
            campaign = Campaign(
              _login = (data \ ("_login")).as[String],
              _token = (data \ ("_token")).as[String],
              network_campaign_id = (data \ ("network_campaign_id")).as[Long].toString(),
              start_date = iso_fmt.parseDateTime((data \ ("start_date")).as[String]),
              daily_budget = (data \ ("daily_budget")).as[Double]))

          if (camp.isDefined) {
            println("!!! SUCCESS postCampaign !!!")

            Created
          } else println("??? FAILED postCampaign ???"); BadRequest
        }
      }
    }
  }

  def getStats = Action { implicit request =>
    { //During the day!!!
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val c = Json.fromJson[Campaign](data \ ("camp"))(Formats.campaign).get

          val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

          val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
          val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

          //Get Statistics from Yandex
          val (statItem_List, json_stat) = API_yandex.getSummaryStat(
            login = c._login,
            token = c._token,
            campaignIDS = List(c.network_campaign_id.toInt),
            start_date = start_date,
            end_date = end_date)

          if (statItem_List.isDefined) {
            println("!!! SUCCESS getStats !!!")

            //Post Statistics to BID
            val res_bid = API_bid.postStats(
              user = User.findByName((data \ ("user")).as[String]).get,
              net = (data \ ("net")).as[String],
              id = c.network_campaign_id,
              performance = Performance.applyStatItem(
                sd = new DateTime(start_date),
                ed = new DateTime(end_date),
                si = statItem_List.get.head))
            if (res_bid.isDefined)
              println("!!! Stats is POSTED to BID !!!")
            else
              println("??? Stats is NOT POSTED to BID ???")

            Ok(Json.toJson(statItem_List.get.head))
          } else println("??? FAILED getStats ???"); BadRequest
        }
      }
    }
  }

  def getReport = Action { implicit request =>
    { //at the END of the day!!!
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {
          val c = Json.fromJson[Campaign](data \ ("camp"))(Formats.campaign).get
         
          val sdf = new SimpleDateFormat("dd MMMMM yyyy - HH:mm", Locale.US)

          val start_date = sdf.parse((data \ ("startDate")).as[String]) //c.start_date
          val end_date = sdf.parse((data \ ("endDate")).as[String]) //new DateTime()

          //create Report on Yandex server
          val newReportID = API_yandex.createNewReport(
            login = c._login,
            token = c._token,
            campaignID = c.network_campaign_id.toInt,
            start_date = start_date,
            end_date = end_date)

          def getUrl: String = {
            try {
              val (short_reportInfo_List, json_reports) = API_yandex.getShortReportList(c._login, c._token)
              println(json_reports)
              val short_reportInfo = short_reportInfo_List.get.filter(_.ReportID == newReportID.get).head
              short_reportInfo.StatusReport match {
                case "Pending" => {
                  Thread.sleep(1000)
                  println("!!!!!! PENDING !!!!!");
                  getUrl
                }
                case "Done" => {
                  println("!!!!!! DONE !!!!!")
                  val reportInfo_List = API_yandex.getReportList(json_reports)
                  val reportInfo = reportInfo_List.get.filter(_.ReportID == newReportID.get).head
                  reportInfo.Url
                }
              }
            } catch {
              case t => {
                println("!!! EMPTY !!!")
                "EMPTY data"
              }
            }
          }

          //Get current report Url 
          val reportUrl = getUrl

          //download XML report from Yandex Url
          val xml_node = API_yandex.getXML(reportUrl)
          println("!!! XML: " + xml_node)

          //post report to BID
          val postToBid = API_bid.postReports(
            user = User.findByName((data \ ("user")).as[String]).get,
            net = (data \ ("net")).as[String],
            id = c.network_campaign_id,
            bannerPhrasePerformance = xml_node)
          if (postToBid.isDefined)
            println("!!! Report is POSTED to BID !!!")
          else
            println("??? Report is NOT POSTED to BID ???")

          //remove current report from Yandex Server
          if (API_yandex.deleteReport(login = c._login, token = c._token, reportID = newReportID.get))
            println("!!! Report is DELETED from Yandex!!!")
          else
            println("??? Report is NOT DELETED from Yandex ???")

          Ok(xml_node) as XML
        }
      }
    }
  }

  def getRecommendations = Action { implicit request =>
    {
      request.body.asJson match {
        case None => BadRequest
        case Some(data) => {

          val user = User.findByName((data \ ("user")).as[String]).get
          val net = (data \ ("net")).as[String]
          val login = (data \ ("_login")).as[String]
          val token = (data \ ("_token")).as[String]
          val id = (data \ ("network_campaign_id")).as[String]

          //get Recommendations from BID
          val ppInfo_List = API_bid.getRecommendations(user, net, id, new DateTime().minusMonths(2))

          if (ppInfo_List.isDefined) {
            println("!!! SUCCESS: Recommendations have TAKEN from BID !!!")
            //Update Prices on Yandex
            implicit lazy val phrasePriceInfo = Json.format[PhrasePriceInfo]
            val res = 
              if (API_yandex.updatePrice(login, token, Json.toJson(ppInfo_List.get)))
                println("SUCCESS: Prices is updated!!!")
              else
                println("FAILED: Prices is NOT updated!!!")

            Ok(Json.toJson(ppInfo_List.get))
          } else println("??? FAILED: Recommendations have NOT TAKEN from BID ???"); BadRequest
        }
      }
    }
  }

  def clearDB = Action {
    if (API_bid.clearDB)
      println("!!! BID DB is CLEAR !!!")
    else
      println("??? FAIL ---> BID DB is NOT CLEAR ???")

    if (User.truncate)
      println("!!! CLIENT DB is CLEAR !!!")
    else
      println("??? FAIL ---> CLIENT DB is NOT CLEAR ???")
    Ok
  }

}