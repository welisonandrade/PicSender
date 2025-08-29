# PicSender - Sistema para envio de imagens

Um projeto simples de cliente-servidor para capturar fotos em um dispositivo móvel (Android e futuramente iOS) e enviá-las via Wi-Fi para um servidor Python que as exibe em tempo real e as armazena em disco.

## Visao Geral
![alt text](/Screeenshots/Captura%20de%20tela%202025-08-29%20072050.png)

## Funcionalidades
- Captura de Imagem: Tira fotos usando a câmera nativa do dispositivo.

- Cliente-Servidor: Envia as imagens capturadas para um servidor central através de uma conexão TCP.

- Visualização em Tempo Real: O servidor, feito em Python, exibe a última foto recebida em uma janela do OpenCV.

- Armazenamento Organizado: As fotos são salvas no servidor em um diretório estruturado por data (data/ANO-MÊS-DIA/).

- Multiplataforma (Planejado): Clientes para Android (pronto) e iOS (em desenvolvimento).

# Arquitetura
O sistema é composto por dois componentes principais:

- Servidor (Python): Uma aplicação server.py que ouve conexões TCP em uma porta específica. Ele é multithreaded, capaz de lidar com várias conexões de clientes. O protocolo de comunicação espera primeiro 4 bytes indicando o tamanho da imagem, seguido pelos bytes da imagem em formato JPEG.

- Clientes (Android / iOS): Aplicativos móveis que se conectam ao servidor, capturam uma foto, a redimensionam para otimizar o envio e a transmitem seguindo o protocolo definido.

# Tecnologias Utilizadas
## Servidor:

- Python 3

- OpenCV (opencv-python)

- Socket e Threading (bibliotecas padrão)

## Cliente Android:

- Kotlin

- Jetpack Compose para a UI

- Kotlin Coroutines para operações em background

- CameraX (via ActivityResultContracts) e FileProvider para captura de imagens

## Cliente iOS (Planejado):

- Swift

- SwiftUI

## Pré-requisitos
Antes de começar, garanta que você tem o seguinte software instalado:

### Para o Servidor:

- Python 3.8+

- pip (gerenciador de pacotes do Python)

### Para o Cliente Android:

- Android Studio (versão mais recente recomendada)

- Para o Cliente iOS:

- Xcode e CocoaPods (quando disponível)

## Instalação e Execução
1. Clone o repositório
``` 
 
git clone <url-do-seu-repositorio>
cd <nome-do-repositorio>/servidor

# 2. Crie e ative um ambiente virtual
python -m venv venv
# No Windows:
venv\Scripts\activate
# No macOS/Linux:
# source venv/bin/activate

# 3. Instale as dependências
pip install opencv-python numpy

# 4. Execute o servidor
python server.py 
```

O terminal deverá exibir a mensagem [SERVIDOR] Ouvindo em 127.0.0.1:5001 ..., indicando que está pronto para receber conexões.

2. Configurando o Cliente Android
- Abra a pasta do projeto Android (/android) no Android Studio.

- Aguarde o Gradle sincronizar as dependências.

- IMPORTANTE: Abra o arquivo app/src/main/java/com/example/picsender/MainActivity.kt.

- Localize a variável ip e altere seu valor padrão para o endereço IP do computador que está rodando o servidor na sua rede local.

- Para testar com o Emulador Android: Use o IP especial 10.0.2.2.

- Para testar com um celular físico: Descubra o IP do seu computador (ex: 192.168.1.10) usando ipconfig (Windows) ou ifconfig (macOS/Linux) e use esse valor.
``` 
 
 // Em MainActivity.kt
var ip by remember { mutableStateOf("10.0.2.2") } 
 
```
- Execute o aplicativo em um emulador ou em um dispositivo físico conectado via USB/ WIFI.

3. Configurando o Cliente iOS
(O cliente iOS ainda está em desenvolvimento. Esta seção será atualizada quando o projeto estiver disponível.)