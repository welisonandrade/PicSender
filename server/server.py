import socket
import struct
import os
from datetime import datetime
import threading
import queue
import cv2
import numpy as np

HOST = os.environ.get("HOST", "127.0.0.1")
PORT = int(os.environ.get("PORT", "5001"))
SAVE_ROOT = "data"
WINDOW_NAME = "Última foto recebida"

# Fila para passar as imagens (bytes JPEG) da thread de rede -> thread de UI
img_queue: "queue.Queue[bytes]" = queue.Queue(maxsize=8)

os.makedirs(SAVE_ROOT, exist_ok=True)

def recv_exact(sock, n: int) -> bytes:
    data = b''
    while len(data) < n:
        packet = sock.recv(n - len(data))
        if not packet:
            raise ConnectionError("Conexão encerrada pelo cliente.")
        data += packet
    return data

def handle_client(conn, addr):
    print(f"[+] Cliente conectado: {addr}")
    try:
        raw_len = recv_exact(conn, 4)
        (length,) = struct.unpack(">I", raw_len)  # uint32 big-endian
        if length <= 0 or length > 50 * 1024 * 1024:
            raise ValueError(f"Tamanho inválido: {length}")

        jpeg_bytes = recv_exact(conn, length)
        # Enfileira para a thread de UI mostrar e salvar.
        try:
            img_queue.put_nowait(jpeg_bytes)
        except queue.Full:
            # Se a fila estiver cheia, descarta a mais antiga e coloca a nova (mantém responsivo)
            _ = img_queue.get_nowait()
            img_queue.put_nowait(jpeg_bytes)
    except Exception as e:
        print(f"[ERRO] {addr}: {e}")
    finally:
        conn.close()
        print(f"[-] Cliente desconectado: {addr}")

def server_loop():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen(5)
        print(f"[SERVIDOR] Ouvindo em {HOST}:{PORT} ...")
        while True:
            conn, addr = s.accept()
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()


def ui_loop():
    # ALTERAÇÃO 1: Mudar para WINDOW_NORMAL para permitir redimensionamento.
    cv2.namedWindow(WINDOW_NAME, cv2.WINDOW_NORMAL)

    # Define uma largura máxima para a imagem exibida, em pixels.
    # Você pode ajustar este valor conforme sua preferência.
    DISPLAY_MAX_WIDTH = 800

    # Cria um placeholder inicial e redimensiona a janela para um tamanho padrão
    placeholder = np.zeros((480, 640, 3), dtype=np.uint8)
    cv2.putText(placeholder, "Aguardando foto...", (10, 240),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2, cv2.LINE_AA)
    cv2.imshow(WINDOW_NAME, placeholder)
    cv2.resizeWindow(WINDOW_NAME, 640, 480)

    while True:
        key = cv2.waitKey(30) & 0xFF
        try:
            if cv2.getWindowProperty(WINDOW_NAME, cv2.WND_PROP_VISIBLE) < 1:
                break
        except cv2.error:
            break

        try:
            jpeg_bytes = img_queue.get_nowait()
        except queue.Empty:
            continue

        # Salva a imagem original em alta resolução (nenhuma mudança aqui)
        date_str = datetime.now().strftime("%Y-%m-%d")
        out_dir = os.path.join(SAVE_ROOT, date_str)
        os.makedirs(out_dir, exist_ok=True)
        ts = datetime.now().strftime("%H%MS")
        out_path = os.path.join(out_dir, f"{ts}.jpg")
        with open(out_path, "wb") as f:
            f.write(jpeg_bytes)
        print(f"[OK] Salvo em: {out_path}")

        # Decodifica para exibir
        img_array = np.frombuffer(jpeg_bytes, dtype=np.uint8)
        img = cv2.imdecode(img_array, cv2.IMREAD_COLOR)
        if img is None:
            print("[ERRO] cv2.imdecode retornou None (JPEG inválido).")
            continue

        # ALTERAÇÃO 2: Redimensiona a imagem para exibição se ela for muito grande.
        # -----------------------------------------------------------------------
        img_h, img_w = img.shape[:2]
        if img_w > DISPLAY_MAX_WIDTH:
            # Calcula a nova altura mantendo a proporção
            ratio = DISPLAY_MAX_WIDTH / img_w
            new_w = DISPLAY_MAX_WIDTH
            new_h = int(img_h * ratio)

            # Redimensiona a imagem usando uma interpolação eficiente para redução (INTER_AREA)
            display_img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)
        else:
            display_img = img
        # -----------------------------------------------------------------------

        # Faixa com nome do arquivo (agora aplicada na imagem redimensionada)
        overlay = display_img.copy()
        cv2.rectangle(overlay, (0, 0), (overlay.shape[1], 30), (0, 0, 0), -1)
        cv2.putText(overlay, f"Recebida: {os.path.basename(out_path)}", (10, 22),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2, cv2.LINE_AA)

        cv2.imshow(WINDOW_NAME, overlay)

    cv2.destroyAllWindows()
if __name__ == "__main__":
    # Inicia servidor em thread DAEMON e roda a UI na thread principal
    threading.Thread(target=server_loop, daemon=True).start()
    try:
        print("Caso vá iniciar com emulador: 10.0.2.2")
        ui_loop()  # bloqueia até a janela ser fechada
    except KeyboardInterrupt:
        pass
    finally:
        cv2.destroyAllWindows()
