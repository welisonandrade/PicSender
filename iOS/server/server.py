import socket, struct, os
from datetime import datetime
import cv2, numpy as np

HOST, PORT = "0.0.0.0", 5001
OUT = os.path.join(os.path.dirname(__file__), "received")
os.makedirs(OUT, exist_ok=True)

def recv_exact(conn, n):
    data = b""
    while len(data) < n:
        chunk = conn.recv(n - len(data))
        if not chunk:
            raise ConnectionError("Conexão encerrada.")
        data += chunk
    return data

def save_and_show(jpeg):
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = os.path.join(OUT, f"foto_{ts}.jpg")
    with open(path, "wb") as f:
        f.write(jpeg)
    print("Imagem salva em:", path)
    arr = np.frombuffer(jpeg, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is not None:
        cv2.imshow("Última imagem recebida", img)
        cv2.waitKey(1)

with socket.socket() as s:
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(1)
    print(f"Servidor escutando em {HOST}:{PORT} (Aguardando foto...)")
    while True:
        conn, addr = s.accept()
        with conn:
            print("Conexão de", addr)
            (length,) = struct.unpack(">I", recv_exact(conn, 4))
            print("Tamanho recebido:", length, "bytes")
            data = recv_exact(conn, length)
            save_and_show(data)
