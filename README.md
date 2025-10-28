**Sistema de Chat Distribuído com Sockets em Java**

Este projeto implementa um sistema de chat cliente-servidor em Java, utilizando sockets para comunicação em rede.

A aplicação atende aos requisitos funcionais de troca de mensagens privadas, conversas em grupo e transferência de arquivos.

**Arquitetura e Decisões Técnicas**

**1. Modelo Cliente-Servidor**
A arquitetura é baseada no clássico modelo cliente-servidor.

**Servidor (ChatServer.java):** Atua como o hub central. Ele é responsável por aceitar conexões de múltiplos clientes, gerenciar sessões de usuários, administrar grupos e rotear todas as mensagens e arquivos para os destinatários corretos. Ele utiliza um ClientHandler para cada cliente conectado.

**Cliente (ChatClient.java):** É a aplicação que o usuário final executa. Ele se conecta ao servidor, permite que o usuário se identifique e fornece a interface (baseada em texto) para enviar e receber dados.

**2. Protocolo de Comunicação:** TCP
A implementação utiliza Sockets TCP (Transmission Control Protocol). A escolha foi feita com base nos seguintes critérios técnicos:

**Confiabilidade:** O TCP é um protocolo orientado à conexão que garante a entrega de todos os pacotes de dados. Para um chat, é crucial que nenhuma mensagem ou parte de um arquivo seja perdida durante a transmissão.

**Ordenação:** O TCP garante que os pacotes de dados cheguem na mesma ordem em que foram enviados. Isso é fundamental para que as mensagens de um diálogo façam sentido e para que os arquivos sejam reconstruídos corretamente no lado do receptor.

**3. Gerenciamento de Concorrência:** Multithreading
O servidor foi projetado para ser multithreaded.

Ao receber uma nova conexão de cliente, a thread principal do servidor cria e inicia uma nova thread (ClientHandler) dedicada exclusivamente a esse cliente.

Isso permite que o servidor gerencie múltiplas conexões simultaneamente sem que um cliente bloqueie o atendimento aos outros.

Para garantir a segurança no acesso a estruturas de dados compartilhadas (como os mapas de clientes e grupos), foram utilizadas coleções do pacote java.util.concurrent, como ConcurrentHashMap, que são seguras para uso em ambientes com múltiplas threads (thread-safe).

** Como Executar o Sistema** 
Pré-requisitos: JDK (Java Development Kit) 8 ou superior instalado e configurado no PATH do sistema.

** Passo 1:**  Compilar os Arquivos
Abra um terminal ou prompt de comando.

Navegue até o diretório onde você salvou os arquivos .java.

Compile todos os arquivos com o seguinte comando:

** javac *.java** 

Isso irá gerar os arquivos .class correspondentes.

** Passo 2:**  Iniciar o Servidor
No mesmo terminal, execute o servidor:

** java ChatServer** 

O servidor será iniciado e exibirá a mensagem: [*] Servidor de chat iniciando na porta 55555.... Ele está pronto para aceitar conexões.

** Passo 3:**  Iniciar Clientes
Abra um novo terminal para cada cliente que você deseja conectar.

Navegue até o mesmo diretório onde os arquivos .class foram gerados.

** Execute o script do cliente:** 

java ChatClient

O programa solicitará um nome de usuário. Digite um nome único e pressione Enter.

Repita os passos 1 a 4 para conectar quantos usuários desejar.

Observação: Se o cliente e o servidor estiverem rodando em máquinas diferentes na mesma rede, altere a variável SERVER_HOST no arquivo ChatClient.java para o endereço IP da máquina onde o servidor está sendo executado e recompile os arquivos.

Funcionalidades e Comandos
Depois de conectado, você pode usar os seguintes comandos e formatos de mensagem:

Mensagem Privada:
@username:Olá, tudo bem?

Criar um Grupo:
/creategroup nome_do_grupo

Entrar em um Grupo:
/joingroup nome_do_grupo

Mensagem em Grupo:
#nome_do_grupo:Olá a todos!

Enviar um Arquivo:
/sendfile @username /caminho/para/o/arquivo.txt
/sendfile #nome_do_grupo /caminho/para/o/arquivo.jpg

Sair do Chat:
/quit
