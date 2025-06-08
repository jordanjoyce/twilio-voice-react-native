package com.twiliovoicereactnative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import expo.modules.core.interfaces.ReactActivityLifecycleListener;


public class ExpoActivityLifecycleListeners implements ReactActivityLifecycleListener {
  VoiceActivityProxy voiceActivityProxy;

  @Override
  public void onCreate(Activity activity, Bundle savedInstanceState) {
    this.voiceActivityProxy = new VoiceActivityProxy(activity, permission -> {
      if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
        Toast.makeText(
          MainActivity.this,
          "Microphone permissions needed. Please allow in your application settings.",
          Toast.LENGTH_LONG).show();
      } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
        Manifest.permission.BLUETOOTH_CONNECT.equals(permission)) {
        Toast.makeText(
          MainActivity.this,
          "Bluetooth permissions needed. Please allow in your application settings.",
          Toast.LENGTH_LONG).show();
      } else if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) &&
        Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
        Toast.makeText(
          MainActivity.this,
          "Notification permissions needed. Please allow in your application settings.",
          Toast.LENGTH_LONG).show();
      }
    });
    
    this.voiceActivityProxy.onCreate(savedInstanceState);
  }

  @Override
  public boolean onNewIntent(Intent intent) {
    this.voiceActivityProxy.onNewIntent(intent);

    return false;
  }

  @Override
  public void onDestroy(Activity activity) {
    this.voiceActivityProxy.onDestroy();
  }
}
