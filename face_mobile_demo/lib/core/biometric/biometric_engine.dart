enum BiometricEngineState { unavailable, ready }

abstract interface class BiometricEngine {
  BiometricEngineState get state;
}

class PlaceholderBiometricEngine implements BiometricEngine {
  @override
  BiometricEngineState get state => BiometricEngineState.unavailable;
}
