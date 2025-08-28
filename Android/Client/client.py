import socket
import struct
import sys

# --- CONFIGURAÇÃO ---
SERVER_HOST = "127.0.0.1"  # Ou o IP do servidor
SERVER_PORT = 5001

def send_image(filepath: str):
    """Lê uma imagem, a empacota e envia para o servidor."""
    try:
        # 1. Lê os bytes da imagem do disco
        with open(filepath, "rb") as f:
            jpeg_bytes = f.read()
    except FileNotFoundError:
        print(f"Erro: Arquivo não encontrado em '{filepath}'")
        return

    # 2. Obtém o tamanho da imagem e o empacota como um uint32 big-endian (4 bytes)
    #    O formato '>I' significa: > (big-endian), I (unsigned int, 4 bytes)
    length_bytes = struct.pack(">I", len(jpeg_bytes))

    # 3. Conecta ao servidor e envia os dados
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            print(f"Conectando a {SERVER_HOST}:{SERVER_PORT}...")
            s.connect((SERVER_HOST, SERVER_PORT))
            print("Conectado. Enviando imagem...")

            # Envia primeiro o tamanho
            s.sendall(length_bytes)
            # Em seguida, envia os bytes da imagem
            s.sendall(jpeg_bytes)

            print(f"Imagem '{filepath}' ({len(jpeg_bytes)} bytes) enviada com sucesso!")
    except ConnectionRefusedError:
        print("Erro: A conexão foi recusada. O servidor está rodando?")
    except Exception as e:
        print(f"Ocorreu um erro: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python client.py <caminho_para_a_imagem.jpg>")
        sys.exit(1)

    image_path = sys.argv[1]
    send_image(image_path)