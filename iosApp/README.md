# iOS Setup for Kotlin Multiplatform

## Configuração do Xcode

Para executar o iosApp, abra o Xcode no macOS e:

1. Abra o projeto `iosApp/iosApp.xcodeproj`
2. Adicione a pasta `iosApp` como "Existing Files" 
3. Configure o Framework gerado pelo KMP:
   - Build Phases → Link Binary With Libraries → Adicionar `Shared` framework
4. Execute `./gradlew :shared:linkReleaseFrameworkIos` para gerar o framework

## Estrutura

```
iosApp/
├── iosApp/
│   ├── Main.swift          # Entry point SwiftUI
│   ├── ContentView.swift   # View principal
│   └── Info.plist          # Configurações iOS
└── iosApp.xcodeproj/       # Projeto Xcode
```

## Framework KMP

O framework Shared é gerado automaticamente pelo Gradle quando compilado no macOS:

```bash
./gradlew :shared:compileKotlinJvm
./gradlew :shared:linkReleaseFrameworkIos
```