import Foundation
import Network

enum SocketError: Error { case invalidHostOrPort }

struct SocketSender {
    static func send(host: String, port: UInt16, payload: Data) async throws {
        guard let nwPort = NWEndpoint.Port(rawValue: port) else { throw SocketError.invalidHostOrPort }
        let connection = NWConnection(host: .init(host), port: nwPort, using: .tcp)

        return try await withCheckedThrowingContinuation { cont in
            func finish(_ err: Error?) {
                connection.cancel()
                if let err { cont.resume(throwing: err) } else { cont.resume(returning: ()) }
            }

            connection.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    var lenBE = UInt32(payload.count).bigEndian
                    let header = Data(bytes: &lenBE, count: 4)
                    connection.send(content: header + payload,
                                    completion: .contentProcessed { e in finish(e) })
                case .failed(let e):
                    finish(e)
                default:
                    break
                }
            }
            connection.start(queue: .global(qos: .userInitiated))
        }
    }
}

