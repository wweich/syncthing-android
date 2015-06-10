package com.nutomic.syncthingandroid.syncthing

import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date

import android.test.AndroidTestCase
import junit.framework.Assert._
import org.joda.time.DateTime
import spray.json._
import DefaultJsonProtocol._

class EventApiTest extends AndroidTestCase {

  private val data =
"""
{
  "id": 1,
  "type": "Starting",
  "time": "2014-07-17T13:13:32.044470055+02:00",
  "data": {
    "home": "/home/jb/.config/syncthing"
  }
}
""".parseJson

  protected override def setUp(): Unit = {
    super.setUp()
  }

  protected override def tearDown(): Unit = {
    super.tearDown()
  }

  def testParsing(): Unit = {
    assertEquals(data, EventApi.parseJson(data))
  }


}