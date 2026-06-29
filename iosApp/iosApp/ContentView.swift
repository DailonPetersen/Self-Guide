import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        VStack {
            Text("SelfGuide Tour")
                .font(.largeTitle)
                .bold()
            
            Text("Tour Guiado Automatizado")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}