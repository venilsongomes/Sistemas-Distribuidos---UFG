import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A classe principal do servidor de chat.
 * Escuta por conexões de clientes e cria um ClientHandler para cada um.
 * Gerencia as listas de clientes e grupos de forma centralizada e thread-safe.
 */
public class ChatServer {
    private static final int PORT = 55555;
    // Mapeia username para o seu handler. Garante segurança em ambiente com múltiplas threads.
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // Mapeia nome do grupo para a lista de handlers dos seus membros.
    private static final Map<String, List<ClientHandler>> groups = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Servidor de chat iniciando na porta " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Aceita uma nova conexão de cliente e cria uma thread para ele
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Não foi possível iniciar o servidor na porta " + PORT);
            e.printStackTrace();
        }
    }

    // --- Métodos de Gerenciamento (para serem acessados pelos ClientHandlers) ---

    public static void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[CONEXÃO] " + username + " conectou-se de " + handler.getSocket().getInetAddress().getHostAddress());
    }

    public static void removeClient(ClientHandler handler) {
        String username = handler.getUsername();
        if (username != null) {
            System.out.println("[DESCONEXÃO] " + username + " desconectou-se.");
            clients.remove(username);
            // Remove o cliente de todos os grupos em que ele participa
            for (String groupName : groups.keySet()) {
                List<ClientHandler> members = groups.get(groupName);
                if (members.remove(handler) && members.isEmpty()) {
                    groups.remove(groupName);
                     System.out.println("[INFO] Grupo '" + groupName + "' ficou vazio e foi removido.");
                }
            }
        }
    }

    public static ClientHandler getClient(String username) {
        return clients.get(username);
    }
    
    public static void createGroup(String groupName, ClientHandler creator) {
        if (!groups.containsKey(groupName)) {
            List<ClientHandler> members = Collections.synchronizedList(new ArrayList<>());
            members.add(creator);
            groups.put(groupName, members);
            creator.sendMessage("[INFO] Grupo '" + groupName + "' criado com sucesso!");
        } else {
            creator.sendMessage("[ERRO] Grupo '" + groupName + "' já existe.");
        }
    }

    public static void joinGroup(String groupName, ClientHandler user) {
        List<ClientHandler> members = groups.get(groupName);
        if (members != null) {
            if (!members.contains(user)) {
                members.add(user);
                user.sendMessage("[INFO] Você entrou no grupo '" + groupName + "'.");
            } else {
                 user.sendMessage("[INFO] Você já é membro do grupo '" + groupName + "'.");
            }
        } else {
            user.sendMessage("[ERRO] Grupo '" + groupName + "' não encontrado.");
        }
    }
    
    public static void routeGroupMessage(String groupName, String message, ClientHandler sender) {
        List<ClientHandler> members = groups.get(groupName);
        if (members != null && members.contains(sender)) {
            String formattedMsg = "[GRUPO " + groupName + " de " + sender.getUsername() + "]: " + message;
            for (ClientHandler member : new ArrayList<>(members)) { // Itera sobre uma cópia para evitar ConcurrentModificationException
                if (member != sender) {
                    member.sendMessage(formattedMsg);
                }
            }
        } else {
            sender.sendMessage("[ERRO] Você não pode enviar mensagem para o grupo '" + groupName + "'.");
        }
    }

    public static void routeGroupFile(String groupName, String filename, byte[] fileData, ClientHandler sender) {
        List<ClientHandler> members = groups.get(groupName);
        if (members != null && members.contains(sender)) {
            System.out.println("[ARQUIVO] Roteando '" + filename + "' para o grupo " + groupName);
            for (ClientHandler member : new ArrayList<>(members)) { // Itera sobre uma cópia
                if (member != sender) {
                    member.sendFile(sender.getUsername(), filename, fileData);
                }
            }
        } else {
             sender.sendMessage("[ERRO] Grupo '" + groupName + "' inválido para envio de arquivo.");
        }
    }
}
