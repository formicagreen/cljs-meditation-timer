#import <Capacitor/Capacitor.h>

CAP_PLUGIN(CustomAudioPlugin, "CustomAudio",
    CAP_PLUGIN_METHOD(play, CAPPluginReturnPromise);
)
