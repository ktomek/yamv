import SwiftUI

struct CounterView: View {
    @StateObject private var viewModel = CounterViewModel()

    var body: some View {
        VStack(spacing: 24) {
            Text("Counter")
                .font(.largeTitle)
                .bold()

            Text("\(viewModel.count)")
                .font(.system(size: 72, weight: .thin, design: .monospaced))

            HStack(spacing: 32) {
                Button(action: viewModel.decrease) {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.red)
                }

                Button(action: viewModel.increase) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.green)
                }
            }
        }
        .padding()
    }
}

#Preview {
    CounterView()
}
