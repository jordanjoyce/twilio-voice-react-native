package com.twiliovoicereactnative

/*
 * A Kotlin rewrite of the original React Native Java module converted to an Expo Module.
 * -----------------------------------------------------------------------------
 * – Requires the Expo Modules API (expo-modules-core) ≥ 1.5.0.
 * – Drop this file in `android/src/main/java/com/twiliovoicereactnative/`.
 * – Update build‑gradle (module) with
 *        implementation("expo.modules:core")
 *        implementation("com.twilio:voice-android:<version>")
 *        implementation("com.google.firebase:firebase-messaging-ktx:<version>")
 * – Register the package in `packages.json` (expo‑modules‑scripts).
 *
 * Only the public surface (methods & events) is ported verbatim.  Where the Java
 * version referenced helpers living elsewhere in the library the equivalent call
 * is marked TODO – copy the existing implementation or create Kotlin helpers.
 *
 * -----------------------------------------------------------------------------*/

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.twilio.audioswitch.AudioDevice
import com.twilio.voice.Call
import com.twilio.voice.CallMessage
import com.twilio.voice.ConnectOptions
import com.twilio.voice.LogLevel
import com.twilio.voice.RegistrationException
import com.twilio.voice.Voice
import expo.modules.kotlin.Promise
import expo.modules.kotlin.events.EventEmitter
import expo.modules.kotlin.events.EventName
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.util.UUID

@OptIn(ExperimentalStdlibApi::class)
class TwilioVoiceModule : Module() {
  /** Expo‑Modules uses a single [EventEmitter] per module. */
  private lateinit var emitter: EventEmitter

  /** Main thread handler for off‑UI calls coming from JS. */
  private val mainHandler = Handler(Looper.getMainLooper())

  /**
   * Lazily initialise your singletons (AudioSwitch manager, DB, …)
   * Port the existing Java helpers or reference them directly if they are
   * already in Kotlin.
   */
  private val audioSwitchManager by lazy { VoiceApplicationProxy.getAudioSwitchManager() }

  /* ----------------------------------------------------------------------- */
  /* Expo Module DEFINITION                                                  */
  /* ----------------------------------------------------------------------- */

  override fun definition() = ModuleDefinition {
    Name("TwilioVoice")

    /* ------------------------------------------------------------------- */
    /* Events – identical names to the Java constants                      */
    /* ------------------------------------------------------------------- */
    Events(
      "voiceEvent" // umbrella event; payload contains VoiceEventType key
    )

    /* ------------------------------------------------------------------- */
    /* Module‑level helpers                                                */
    /* ------------------------------------------------------------------- */

    OnCreate {
      /**
       * Equivalent to the Java constructor.  Set environment properties &
       * initialise Twilio.
       */
      System.setProperty("com.twilio.voice.env", CommonConstants.ReactNativeVoiceSDK)
      System.setProperty("com.twilio.voice.env.sdk.version", CommonConstants.ReactNativeVoiceSDKVer)
      Voice.setLogLevel(if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR)

      emitter = this@TwilioVoiceModule.eventEmitter

      // Forward AudioSwitch updates as JS events.
      audioSwitchManager.setListener { devices, selectedUuid, selectedDevice ->
        val payload = ReactNativeArgumentsSerializer.serializeAudioDeviceInfo(
          devices,
          selectedUuid,
          selectedDevice
        ).apply {
          putString(CommonConstants.VoiceEventType, CommonConstants.VoiceEventAudioDevicesUpdated)
        }
        sendEvent("voiceEvent", payload)
      }
    }

    /* ------------------------------------------------------------------- */
    /* Async functions (formerly @ReactMethod with Promise)                */
    /* ------------------------------------------------------------------- */

    AsyncFunction("voice_connect_android") { accessToken: String,
                                             twimlParams: Map<String, Any?>,
                                             notificationDisplayName: String,
                                             promise: Promise ->
      mainHandler.post {
        val parsed = twimlParams.mapValues { it.value.toString() } as HashMap<String, String>
        val uuid = UUID.randomUUID()

        val callRecipient = parsed["to"].takeUnless { it.isNullOrBlank() }
          ?: appContext.reactContext.getString(R.string.unknown_call_recipient)

        val connectOptions = ConnectOptions.Builder(accessToken)
          .enableDscp(true)
          .params(parsed)
          .callMessageListener(CallMessageListenerProxy())
          .build()
        try {
          val callRecord = CallRecordDatabase.CallRecord(
            uuid,
            VoiceApplicationProxy.getVoiceServiceApi().connect(
              connectOptions,
              CallListenerProxy(uuid, VoiceApplicationProxy.getVoiceServiceApi().serviceContext)
            ),
            callRecipient,
            parsed,
            CallRecordDatabase.CallRecord.Direction.OUTGOING,
            notificationDisplayName
          )

          VoiceApplicationProxy.getCallRecordDatabase().add(callRecord)
          promise.resolve(ReactNativeArgumentsSerializer.serializeCall(callRecord))
        } catch (e: SecurityException) {
          promise.reject(e, ReactNativeArgumentsSerializer.serializeError(31401, e.message))
        }
      }
    }

    AsyncFunction("voice_getVersion") { ->
      Voice.getVersion()  // return value automatically resolves the promise
    }

    AsyncFunction("voice_getDeviceToken") { promise: Promise ->
      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
          promise.reject("FCM_TOKEN", "Failed to get FCM token: ${task.exception?.message}")
          return@addOnCompleteListener
        }
        promise.resolve(task.result)
      }
    }

    AsyncFunction("voice_getCalls") { ->
      val calls = VoiceApplicationProxy.getCallRecordDatabase().collection.mapNotNull { rec ->
        rec.voiceCall?.let { ReactNativeArgumentsSerializer.serializeCall(rec) }
      }
      calls
    }

    AsyncFunction("voice_getAudioDevices") { ->
      val devices = audioSwitchManager.audioDevices
      val selectedUuid = audioSwitchManager.selectedAudioDeviceUuid
      val selectedDevice = audioSwitchManager.selectedAudioDevice
      ReactNativeArgumentsSerializer.serializeAudioDeviceInfo(devices, selectedUuid, selectedDevice)
    }

    AsyncFunction("voice_selectAudioDevice") { uuid: String ->
      val audioDevice = audioSwitchManager.audioDevices[uuid]
        ?: throw IllegalArgumentException(appContext.reactContext.getString(R.string.missing_audiodevice_uuid, uuid))
      audioSwitchManager.audioSwitch.selectDevice(audioDevice)
      null // resolve with void
    }

    /* ------------------------------------------------------------------- */
    /* Example – re‑implemented call helper                                */
    /* ------------------------------------------------------------------- */

    AsyncFunction("call_mute") { uuid: String, mute: Boolean ->
      val callRecord = validateCallRecord(UUID.fromString(uuid))
      callRecord.voiceCall?.mute(mute)
      callRecord.voiceCall?.isMuted
    }

    /* ------------------------------------------------------------------- */
    /* System helpers                                                      */
    /* ------------------------------------------------------------------- */

    AsyncFunction("system_isFullScreenNotificationEnabled") { ->
      val ctx = appContext.reactContext
      val enabled = ConfigurationProperties.isFullScreenNotificationEnabled(ctx) &&
        NotificationManagerCompat.from(ctx).canUseFullScreenIntent()
      enabled
    }

    AsyncFunction("system_requestFullScreenNotificationPermission") { ->
      val ctx = appContext.reactContext
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU &&
        ConfigurationProperties.isFullScreenNotificationEnabled(ctx)) {
        val intent = Intent(
          Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
          Uri.parse("package:${ctx.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent)
      }
      null
    }
  }

  /* --------------------------------------------------------------------- */
  /* Helpers                                                               */
  /* --------------------------------------------------------------------- */

  private fun sendEvent(event: String, payload: Any?) {
    emitter.emit(event, payload)
  }

  private fun validateCallRecord(uuid: UUID): CallRecordDatabase.CallRecord {
    return VoiceApplicationProxy.getCallRecordDatabase().get(CallRecordDatabase.CallRecord(uuid))
      ?: throw IllegalArgumentException(appContext.reactContext.getString(R.string.missing_call_uuid, uuid))
  }

  // region – enums mapping identical to Java helper functions ------------ //
  private val scoreMap = mapOf(
    CommonConstants.CallFeedbackScoreNotReported to Call.Score.NOT_REPORTED,
    CommonConstants.CallFeedbackScoreOne to Call.Score.ONE,
    CommonConstants.CallFeedbackScoreTwo to Call.Score.TWO,
    CommonConstants.CallFeedbackScoreThree to Call.Score.THREE,
    CommonConstants.CallFeedbackScoreFour to Call.Score.FOUR,
    CommonConstants.CallFeedbackScoreFive to Call.Score.FIVE
  )

  private val issueMap = mapOf(
    CommonConstants.CallFeedbackIssueAudioLatency to Call.Issue.AUDIO_LATENCY,
    CommonConstants.CallFeedbackIssueChoppyAudio to Call.Issue.CHOPPY_AUDIO,
    CommonConstants.CallFeedbackIssueEcho to Call.Issue.ECHO,
    CommonConstants.CallFeedbackIssueDroppedCall to Call.Issue.DROPPED_CALL,
    CommonConstants.CallFeedbackIssueNoisyCall to Call.Issue.NOISY_CALL,
    CommonConstants.CallFeedbackIssueNotReported to Call.Issue.NOT_REPORTED,
    CommonConstants.CallFeedbackIssueOneWayAudio to Call.Issue.ONE_WAY_AUDIO
  )

  private fun getScoreFromString(score: String?): Call.Score =
    scoreMap[score] ?: Call.Score.NOT_REPORTED

  private fun getIssueFromString(issue: String?): Call.Issue =
    issueMap[issue] ?: Call.Issue.NOT_REPORTED
  // endregion ------------------------------------------------------------ //
}
