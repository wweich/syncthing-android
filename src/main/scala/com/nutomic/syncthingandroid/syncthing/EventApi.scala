package com.nutomic.syncthingandroid.syncthing

import android.content.Context
import android.os.Handler
import android.util.Log
import org.joda.time.DateTime
import spray.json._
import DefaultJsonProtocol._

object EventApi {

  trait EventListener {
    def onEvent()
  }

  trait Event {
    val id: Int
    val time: DateTime
  }

  case class ConfigSaved       (id: Int, time: DateTime) extends Event // TODO: missing data
  case class DeviceConnected   (id: Int, time: DateTime, addr: String, device: String) extends Event
  case class DeviceDisconnected(id: Int, time: DateTime, error: String, device: String) extends Event
  case class DeviceDiscovered  (id: Int, time: DateTime, addrs: Seq[String], device: String) extends Event
  case class DeviceRejected    (id: Int, time: DateTime, address: String, device: String) extends Event
  case class DownloadProgress  (id: Int, time: DateTime) extends Event // TODO: missing data
  case class FolderCompletion  (id: Int, time: DateTime, completion: Int, device: String, folder: String) extends Event
  case class FolderRejected    (id: Int, time: DateTime, device: String, folder: String) extends Event
  case class FolderSummary     (id: Int, time: DateTime, folder: String) extends Event // TODO: missing data
  case class ItemFinished      (id: Int, time: DateTime, item: String, folder: String, error: Option[String], itemType: String,
                                action: String) extends Event
  case class ItemStarted       (id: Int, time: DateTime, item: String, folder: String, error: Option[String], itemType: String,
                                action: String) extends Event
  case class LocalIndexUpdated (id: Int, time: DateTime, folder: String, items: Int) extends Event
  case class Ping              (id: Int, time: DateTime) extends Event
  case class RemoteIndexUpdated(id: Int, time: DateTime, device: String, folder: String, items: Int) extends Event
  case class Starting          (id: Int, time: DateTime, home: String) extends Event
  case class StartupCompleted  (id: Int, time: DateTime) extends Event
  case class StateChanged      (id: Int, time: DateTime, folder: String, from: String, duration: Float, to: String) extends Event

  implicit def convertInt   (value: JsValue): Int = value.convertTo[Int]
  implicit def convertFloat (value: JsValue): Float = value.convertTo[Float]
  implicit def convertString(value: JsValue): String = value.convertTo[String]
  implicit def convertStringOption(value: JsValue): Option[String] = value.convertTo[Option[String]]
  implicit def convertStringSeq(value: JsValue): Seq[String] = value.convertTo[Seq[String]]
  implicit def convertData(value: JsValue): DateTime = new DateTime(convertString(value))

  def parseJson(json: JsValue): Event = {
    val map  = json.asJsObject.fields
    val id   = map("id").convertTo[Int]
    val time = map("time").convertTo[DateTime]
    val data = map("data").asJsObject.fields
    map("type").convertTo[String] match {
      case "ConfigSaved"        => ConfigSaved(id, time)
      case "DeviceConnected"    => DeviceConnected(id, time, data("addr"), data("id"))
      case "DeviceDisconnected" => DeviceDisconnected(id, time, data("error"), data("id"))
      case "DeviceDiscovered"   => DeviceDiscovered(id, time, data("addrs"), data("device"))
      case "DeviceRejected"     => DeviceRejected(id, time, data("address"), data("device"))
      case "DownloadProgress"   => DownloadProgress(id, time)
      case "FolderCompletion"   => FolderCompletion(id, time, data("completion"), data("device"),
                                                      data("folder"))
      case "FolderRejected"     => FolderRejected(id, time, data("device"), data("folder"))
      case "FolderSummary"      => FolderSummary(id, time, data("folder"))
      case "ItemFinished"       => ItemFinished(id, time, data("item"), data("folder"),
                                                data("error"), data("type"), data("action"))
      case "ItemStarted"        => ItemStarted(id, time, data("item"), data("folder"),
                                                      data("error"), data("type"), data("action"))
      case "LocalIndexUpdated"  => LocalIndexUpdated(id, time, data("folder"), data("items"))
      case "Ping"               => Ping(id, time)
      case "RemoteIndexUpdated" => RemoteIndexUpdated(id, time, data("device"), data("folder"),
                                                      data("items"))
      case "Starting"           => Starting(id, time, data("home"))
      case "StartupCompleted"   => StartupCompleted(id, time)
      case "StateChanged"       => StateChanged(id, time, data("folder"), data("from"),
                                                      data("duration"), data("to"))
    }
  }

}

// alternative libs:
// http://stackoverflow.com/a/14442630
class EventApi(context: Context, url: String, apiKey: String, guiUser: String, guiPassword: String) {

  private lazy val handler = new Handler()

  private var cancelled = false

  private var lastId = 1

  poll()

  private def poll(): Unit = {
    if (cancelled)
      return

    handler.postDelayed(new Runnable {
      override def run(): Unit = {
        pollTask.execute(url, GetTask.URI_EVENTS, apiKey, "since", lastId.toString)
        poll()
      }
    }, SyncthingService.GUI_UPDATE_INTERVAL)
  }

  private class EventException extends RuntimeException("Failed to parse events")

  private val pollTask = new GetTask(context.getFilesDir + "/" + SyncthingService.HTTPS_CERT_FILE) {
    override def onPostExecute(result: String): Unit = {
      // TODO: result is wrapped in array?
      EventApi.parseJson(result.parseJson).foreach {
        // TODO: save id, call listeners
      }
    }
  }

  def cancel(): Unit = cancelled = true

}
