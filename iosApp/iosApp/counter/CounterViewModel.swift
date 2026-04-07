import Foundation
import Combine
import Yamv

@MainActor
class CounterViewModel: ObservableObject {
    @Published var count: Int = 0
    @Published var autoIncreaseOn: Bool = false
    @Published var autoDecreaseOn: Bool = false

    private let runtime: MviRuntime<CounterState>
    private var cancellables = Set<AnyCancellable>()

    init() {
        let features: Set<AnyObject> = [
            IncreaseFeatureKt.increaseFeature,
            DecreaseFeature(),
        ]
        runtime = MviRuntimeKt.MviRuntime(
            features: features as! Set<AnyObject>,
            defaultState: CounterState()
        )
        observeState()
    }

    private func observeState() {
        // Collect StateFlow using Combine bridge
        // StateFlow.value is directly accessible from Swift
        // For live updates, use a polling approach or SKIE
        Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                let state = self.runtime.state.value as! CounterState
                self.count = Int(state.count)
                self.autoIncreaseOn = state.autoIncreaseOn
                self.autoDecreaseOn = state.autoDecreaseOn
            }
            .store(in: &cancellables)
    }

    func increase() { runtime.dispatch(intention: IncreaseCounterIntention()) }
    func decrease() { runtime.dispatch(intention: DecreaseCounterIntention()) }
}
