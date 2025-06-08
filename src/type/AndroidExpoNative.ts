export interface AndroidExpoNative {
  voice_connect_android(
    accessToken: string,
    params: Record<string, string>,
    notificationDisplayName: string
  ): Promise<any>;
  voice_getVersion(): Promise<string>;
  // …all other functions
}