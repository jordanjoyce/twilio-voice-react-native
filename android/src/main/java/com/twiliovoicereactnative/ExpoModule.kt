package com.twiliovoicereactnative

import android.content.Context
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import com.twilio.voice.Voice
import com.twilio.voice.ConnectOptions
import java.util.HashMap
import java.util.UUID

import com.twiliovoicereactnative.CallRecordDatabase.CallRecord


class ExpoModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("TwilioVoiceExpo")

    Function("voice_connect") { accessToken: String, twimlParams: HashMap<String, String>?, calleeName: String, displayName: String ->
      val context = appContext.reactContext ?: return@Function null

      val connectOptions = ConnectOptions.Builder(accessToken)

      if (twimlParams != null) {
        connectOptions.params(twimlParams)
      }

      val uuid = UUID.randomUUID()
      val callListenerProxy = CallListenerProxy(uuid, context)

      val call = VoiceApplicationProxy.getVoiceServiceApi().connect(
        connectOptions.build(),
        callListenerProxy
      )

      val callRecord = CallRecordDatabase.CallRecord(
        uuid,
        call,
        calleeName,
        twimlParams ?: HashMap(),
        CallRecord.Direction.OUTGOING,
        displayName
      )

      VoiceApplicationProxy.getCallRecordDatabase().add(callRecord)

      return@Function uuid.toString()
    }
  }
}
