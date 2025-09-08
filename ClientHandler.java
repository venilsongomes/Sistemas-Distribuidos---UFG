import java.io.*;
import java.net.*;

/**
 * Lida com toda a comunicação para um único cliente em uma thread separada.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String username;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return clientSocket;
    }

    @Override
    public void run() {
        try {
            // Inicializa os streams de comunicação
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            dataIn = new DataInputStream(clientSocket.getInputStream());
            dataOut = new DataOutputStream(clientSocket.getOutputStream());

            // A primeira mensagem recebida deve ser o nome de usuário
            this.username = in.readLine();
            if (username == null || username.trim().isEmpty() || ChatServer.getClient(username) != null) {
                sendMessage("[ERRO] Nome de usuário inválido ou já em uso.");
                clientSocket.close();
                return;
            }

            ChatServer.addClient(this.username, this);

            // Envia mensagens de boas-vindas e instruções
            sendMessage("Bem-vindo ao chat, " + username + "!");

            // Loop principal para ler mensagens do cliente
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleCommand(inputLine);
            }

        } catch (IOException e) {
            // Acontece quando o cliente se desconecta abruptamente
        } finally {
            // Bloco de limpeza para garantir que o cliente seja removido
            try {
                ChatServer.removeClient(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        if (command.startsWith("@")) { // Mensagem privada
            String[] parts = command.substring(1).split(":", 2);
            if (parts.length == 2) {
                ClientHandler recipient = ChatServer.getClient(parts[0]);
                if (recipient != null) {
                    recipient.sendMessage("[PRIVADO de " + username + "]: " + parts[1]);
                } else {
                    sendMessage("[ERRO] Usuário '" + parts[0] + "' não encontrado ou offline.");
                }
            }
        } else if (command.startsWith("#")) { // Mensagem de grupo
            String[] parts = command.substring(1).split(":", 2);
            if (parts.length == 2) {
                ChatServer.routeGroupMessage(parts[0], parts[1], this);
            }
        } else if (command.startsWith("/creategroup ")) {
            String groupName = command.substring(13).trim();
            ChatServer.createGroup(groupName, this);
        } else if (command.startsWith("/joingroup ")) {
            String groupName = command.substring(11).trim();
            ChatServer.joinGroup(groupName, this);
        } else if (command.startsWith("/sendfile ")) {
            // Protocolo (visto pelo servidor): /sendfile <dest> <filename> <filesize>
            String[] parts = command.split(" ", 4);
            String dest = parts[1]; // @user ou #group
            String filename = parts[2];
            long filesize = Long.parseLong(parts[3]);

            System.out.println("[ARQUIVO] Recebendo '" + filename + "' (" + filesize + " bytes) de " + username);

            // Lê os dados do arquivo diretamente do socket do cliente
            byte[] fileData = new byte[(int) filesize];
            dataIn.readFully(fileData, 0, (int) filesize);

            System.out.println("[ARQUIVO] '" + filename + "' recebido de " + username + ". Roteando...");

            // Roteia o arquivo para o destino correto
            if (dest.startsWith("@")) {
                String recipientName = dest.substring(1);
                ClientHandler recipient = ChatServer.getClient(recipientName);
                if (recipient != null) {
                    recipient.sendFile(this.username, filename, fileData);
                } else {
                    sendMessage("[ERRO] Usuário '" + recipientName + "' não encontrado para envio de arquivo.");
                }
            } else if (dest.startsWith("#")) {
                String groupName = dest.substring(1);
                ChatServer.routeGroupFile(groupName, filename, fileData, this);
            }
        }
    }

    /** Envia uma mensagem de texto para este cliente. */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Envia um arquivo para este cliente (sincronizado para evitar corrupção de
     * dados).
     */
    public synchronized void sendFile(String sender, String filename, byte[] fileData) {
        try {
            // 1. Envia o cabeçalho de recebimento de arquivo
            sendMessage("/recvfile " + sender + " " + filename + " " + fileData.length);
            // 2. Envia os dados binários do arquivo
            dataOut.write(fileData, 0, fileData.length);
            dataOut.flush();
            System.out.println("[ARQUIVO] '" + filename + "' enviado para " + this.username);
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao enviar arquivo para " + this.username);
            e.printStackTrace();
        }
    }
}
