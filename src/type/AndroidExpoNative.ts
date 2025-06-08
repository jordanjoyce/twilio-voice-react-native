/* eslint-disable @typescript-eslint/consistent-type-definitions */

import { NativeAudioDeviceInfo } from './AudioDevice';
import { NativeCallInfo } from './Call';
import { NativeCallInviteInfo } from './CallInvite';

/**
 * Typed surface of the TwilioVoice Expo Module (Android-only).
 * Matches every function exposed in TwilioVoiceModule.kt.
 */
/** Map<string,string> that Kotlin receives after `Arguments.toBundle()` */
export type StringMap = Record<string, string>;

/** Statistics object returned by `call_getStats` (SDK-defined shape) */
export interface CallStats {
  /* shape defined by Twilio’s getStats; use `any` if you don’t need TS help */
  [key: string]: any;
}

/** 
 * Direct binding to the Expo native module.
 * Each method mirrors its Java/Kotlin counterpart and *always* returns a Promise.
 */
export interface AndroidExpoNative {
  /* ───────────── Voice-level methods ───────────── */

  voice_connect_android(
    accessToken: string,
    params: StringMap,
    notificationDisplayName?: string,
  ): Promise<NativeCallInfo>;

  voice_getVersion(): Promise<string>;

  voice_getDeviceToken(): Promise<string>;

  voice_showNativeAvRoutePicker(): Promise<void>; // no-op on Android

  voice_getCalls(): Promise<NativeCallInfo[]>;

  voice_getCallInvites(): Promise<NativeCallInviteInfo[]>;

  voice_getAudioDevices(): Promise<{
    audioDevices: NativeAudioDeviceInfo[];
    selectedDevice?: NativeAudioDeviceInfo | null;
  }>;

  voice_selectAudioDevice(uuid: string): Promise<void>;

  voice_setIncomingCallContactHandleTemplate(template?: string): Promise<void>;

  voice_register(token: string): Promise<void>;

  voice_unregister(token: string): Promise<void>;

  voice_handleEvent(messageData: StringMap): Promise<boolean>;

  /* ───────────── Call-level methods ───────────── */

  call_getState(
    uuid: string
  ): Promise<
    | 'queued'
    | 'ringing'
    | 'connecting'
    | 'connected'
    | 'reconnecting'
    | 'disconnecting'
    | 'disconnected'
  >;

  call_isMuted(uuid: string): Promise<boolean>;

  call_isOnHold(uuid: string): Promise<boolean>;

  call_disconnect(uuid: string): Promise<string>; // resolves with same uuid

  call_hold(uuid: string, hold: boolean): Promise<boolean>; // returns new hold state

  call_mute(uuid: string, mute: boolean): Promise<boolean>; // returns new mute state

  call_sendDigits(uuid: string, digits: string): Promise<string>;

  call_postFeedback(
    uuid: string,
    score: string, // 'NOT_REPORTED'|'ONE'|'TWO'|'THREE'|'FOUR'|'FIVE'
    issue: string // see Call.Issue enum
  ): Promise<string>;

  call_getStats(uuid: string): Promise<CallStats>;

  call_sendMessage(
    uuid: string,
    content: string,
    contentType: string,
    messageType: string
  ): Promise<number>; // returns message SID (int) per SDK docs

  /* ───────────── CallInvite methods ───────────── */

  callInvite_accept(
    callInviteUuid: string,
    options: Record<string, unknown>, // reserved for future
  ): Promise<void>;

  callInvite_reject(callInviteUuid: string): Promise<void>;

  /* ───────────── System helpers ───────────── */

  system_isFullScreenNotificationEnabled(): Promise<boolean>;

  system_requestFullScreenNotificationPermission(): Promise<void>;
}