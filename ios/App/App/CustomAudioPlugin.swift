// Native code for playing audio
// Playing audio through the dom api is kind of unreliable, so we have to call out to Swift
// There is a Capacitor plugin for this as well but it doesn't work
// https://capacitorjs.com/docs/ios/custom-code

import Capacitor
import AVFoundation

var player: AVAudioPlayer?

@objc(CustomAudioPlugin)
public class CustomAudioPlugin: CAPPlugin {
    @objc func play(_ call: CAPPluginCall) {
            print("test")
            guard let fileName = call.getString("fileName") else {
                call.reject("No file name supplied")
                return
            }

            // Constructing a URL object from the fileName string
            let fileURL = URL(fileURLWithPath: fileName)
            let resourcePath = "public/" + fileURL.deletingPathExtension().lastPathComponent

            guard let url = Bundle.main.url(forResource: resourcePath, withExtension: fileURL.pathExtension) else {
                call.reject("Could not load audio")
                return
            }

            do {
                try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                try AVAudioSession.sharedInstance().setActive(true)

                player = try AVAudioPlayer(contentsOf: url, fileTypeHint: AVFileType.wav.rawValue)
                guard let player = player else {
                    call.reject("Audio player not initialized")
                    return
                }

                player.play()
                call.resolve(["success": "sound played"])

            } catch let error {
                call.reject(error.localizedDescription)
            }

    }
}

