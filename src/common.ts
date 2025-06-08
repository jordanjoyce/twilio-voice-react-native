/**
 * Cross-platform bridge chooser
 * – Expo-Module on Android if present
 * – React-Native bridge everywhere else
 */

import {
  NativeModules,
  NativeEventEmitter as RNNativeEventEmitter,
  Platform as RNPlatform,
} from 'react-native';
import {
  requireNativeModule,
  EventEmitter as ExpoEventEmitter,
} from 'expo-modules-core';

import type { TwilioVoiceReactNative as RNBridgeType } from './type/NativeModule';
import type { AndroidExpoNative } from './type/AndroidExpoNative'; 

// ─────────── constants ────────────
const EXPO_ANDROID_NAME = 'TwilioVoice'; // Name("TwilioVoice") in Kotlin
const RN_BRIDGE_NAME = 'TwilioVoiceReactNative';

// ─────────── native handles ────────
const ExpoAndroid: AndroidExpoNative | null =
  RNPlatform.OS === 'android'
    ? (() => {
        try {
          return requireNativeModule<AndroidExpoNative>(EXPO_ANDROID_NAME);
        } catch {
          return null; // Module not present (old build)
        }
      })()
    : null;

const RNBridge = NativeModules[RN_BRIDGE_NAME] as RNBridgeType | undefined;

// ─────────── public exports ────────
export const NativeModule: AndroidExpoNative | RNBridgeType = (ExpoAndroid ??
  RNBridge)!;

export const NativeEventEmitter =
  ExpoAndroid && RNPlatform.OS === 'android'
    ? new ExpoEventEmitter(ExpoAndroid) // Expo Modules event emitter
    : new RNNativeEventEmitter(NativeModule as any);

export const Platform = RNPlatform;