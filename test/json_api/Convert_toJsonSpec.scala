package json_api

import models._

import common.Yandex
import json_api.Convert._
import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import org.joda.time._

class Convert_toJsonSpec extends Specification with AllExpectations {

  /*------------- ShortCampaignInfo ---------------------------------------------------*/
  "toJson - ShortCampaignInfo" should {
    sequential

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")

      val data = ShortCampaignInfo(
        CampaignID = 100,
        Login = "krisp0",
        Name = "some_name",
        StartDate = new DateTime(date),
        Sum = 10.3,
        Rest = 20.1,
        SumAvailableForTransfer = Some(30.0),
        Shows = 100,
        Clicks = 10,
        Status = Some(""),
        StatusModerate = None,
        IsActive = Some("yes"),
        ManagerName = Some("manager"),
        AgencyName = None)

      val res = toJson[ShortCampaignInfo](data)

      res \ "CampaignID" must_== (JsNumber(100))
      res \ "Login" must_== (JsString("krisp0"))
      res \ "Sum" must_== (JsNumber(10.3))
      (res \ "Status").asOpt[String] must_== (Some(""))
      (res \ "StartDate").as[DateTime] must_== (new DateTime(date))
      (res \ "ManagerName").asOpt[String] must_== (Some("manager"))
      (res \ "AgencyName").asOpt[String] must_== (None)

    }
  }

  /*------------- List[ShortCampaignInfo] ---------------------------------------------------*/
  "toJson - List[ShortCampaignInfo]" should {
    sequential

    "take TRUE data" in {
      val date = Yandex.date_fmt.parse("2013-01-01")
      val sci = ShortCampaignInfo(
        CampaignID = 100,
        Login = "krisp0",
        Name = "some_name",
        StartDate = new DateTime(date),
        Sum = 10.3,
        Rest = 20.1,
        SumAvailableForTransfer = Some(30.0),
        Shows = 100,
        Clicks = 10,
        Status = Some(""),
        StatusModerate = None,
        IsActive = Some("yes"),
        ManagerName = Some("manager"),
        AgencyName = None)
      val data = List(sci, sci)

      val res = toJson[List[ShortCampaignInfo]](data)

      res \\ "CampaignID" map (_.as[Int]) must_== (List(100, 100))
      res \\ "Login" map (_.as[String]) must_== (List("krisp0", "krisp0"))
      res \\ "Sum" must_== (List(JsNumber(10.3), JsNumber(10.3)))
      res \\ "ManagerName" map (_.asOpt[String]) must_== (List(Some("manager"), Some("manager")))
      res \\ "AgencyName" must_== (List())

    }
  }

  /*------------- BannerInfo ---------------------------------------------------*/
  "toJson - BannerInfo" should {
    sequential

    "take TRUE data" in {
      val bphi = BannerPhraseInfo(Phrase = "some_phrase")
      val data = BannerInfo(
        BannerID = 100,
        Text = "some",
        Geo = "12, 11",
        Phrases = List(bphi, bphi))

      val res = toJson[BannerInfo](data)

      res \ "BannerID" must_== (JsNumber(100))
      res \ "Text" must_== (JsString("some"))
      (res \ "Geo").as[String] must_== ("12, 11")
      res \ "Phrases" \\ "CampaignID" must_== (List(JsNumber(0), JsNumber(0)))
      res \ "Phrases" \\ "Phrase" map (_.as[String]) must_== (List("some_phrase", "some_phrase"))
    }
  }

  /*------------- List[BannerInfo] ---------------------------------------------------*/
  "toJson - List[BannerInfo]" should {
    sequential

    "take TRUE data" in {
      val bphi = BannerPhraseInfo(Phrase = "some_phrase")
      val bi = BannerInfo(
        BannerID = 100,
        Text = "some",
        Geo = "12, 11",
        Phrases = List(bphi, bphi))
      val data = List(bi, bi)

      val res = toJson[List[BannerInfo]](data)

      res \\ "Text" map (_.as[String]) must_== (List("some", "some"))
      (res \\ "Phrases").head \\ "Phrase" map (_.as[String]) must_== (List("some_phrase", "some_phrase"))
    }
  }
}