import SwiftUI

struct ContentView: View {
    @State private var serverIP = ""          // ex.: 192.168.0.12
    @State private var serverPort = "5001"    // mesma porta do server.py
    @State private var showingCamera = false
    @State private var isSending = false
    @State private var statusMessage = "Informe IP/porta e toque em Tirar e Enviar."
    @State private var lastImage: UIImage?

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                TextField("IP do servidor (ex: 192.168.0.12)", text: $serverIP)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .keyboardType(.numbersAndPunctuation)
                    .textFieldStyle(.roundedBorder)

                TextField("Porta (ex: 5001)", text: $serverPort)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)

                Button {
                    showingCamera = true
                } label: { Label("Tirar e Enviar", systemImage: "camera") }
                .buttonStyle(.borderedProminent)
                .disabled(isSending || serverIP.isEmpty || serverPort.isEmpty)

                if isSending { ProgressView("Enviando...") }

                if let img = lastImage {
                    Image(uiImage: img)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 240)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(.gray.opacity(0.3)))
                }

                Text(statusMessage)
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Spacer()
            }
            .padding()
            .navigationTitle("Foto → Servidor")
            .sheet(isPresented: $showingCamera) {
                CameraPicker { uiImage in
                    showingCamera = false
                    guard let uiImage else { return }
                    lastImage = uiImage
                    Task { await sendImage(uiImage) }
                }
            }
        }
    }

    @MainActor
    func sendImage(_ image: UIImage) async {
        guard let portNum = UInt16(serverPort) else {
            statusMessage = "Porta inválida."
            return
        }
        isSending = true
        defer { isSending = false }

        let resized = image.resized(maxWidth: 1280) ?? image
        guard let jpeg = resized.jpegData(compressionQuality: 0.8) else {
            statusMessage = "Falha ao gerar JPEG."
            return
        }

        do {
            try await SocketSender.send(host: serverIP, port: portNum, payload: jpeg)
            statusMessage = "Enviado com sucesso! (\(jpeg.count) bytes)"
        } catch {
            statusMessage = "Erro ao enviar: \(error.localizedDescription)"
        }
    }
}

