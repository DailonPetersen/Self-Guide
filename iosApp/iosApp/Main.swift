import SwiftUI
import Shared

@main
struct SelfGuideApp: App {
    init() {
        InitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}