package com.twiliovoicereactnative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import expo.modules.core.interfaces.ReactActivityLifecycleListener;


public class ExpoActivityLifecycleListeners implements ReactActivityLifecycleListener {
  VoiceActivityProxy voiceActivityProxy;

  @Override
  public void onCreate(Activity activity, Bundle savedInstanceState) {
    this.voiceActivityProxy = new VoiceActivityProxy(activity, null);
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
