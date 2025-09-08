
// Importa classes para entrada/saída de dados
import java.io.*;
// Importa classes para comunicação via rede
import java.net.*;
// Importa classe para ler entrada do usuário
import java.util.Scanner;

// Classe principal do cliente de chat
public class ChatClient {
    // Endereço do servidor
    private static final String SERVER_HOST = "127.0.0.1";
    // Porta do servidor
    private static final int SERVER_PORT = 55555;

    // Método principal
    public static void main(String[] args) {
        // Tenta conectar ao servidor e inicializar recursos
        try (
                // Cria o socket para conectar ao servidor
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                // Cria o escritor para enviar mensagens ao servidor
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // Scanner para ler entrada do usuário
                Scanner consoleInput = new Scanner(System.in)) {
            // Solicita o nome de usuário ao usuário
            System.out.print("Digite seu nome de usuário: ");
            String username = consoleInput.nextLine();
            // Envia o nome de usuário ao servidor
            out.println(username);

            // Cria e inicia a thread para receber mensagens do servidor
            ReceiverThread receiver = new ReceiverThread(socket);
            new Thread(receiver).start();

            // A thread principal fica responsável por enviar mensagens para o servidor
            System.out.println("--- Conectado ao chat. Digite /quit para sair ---");
            String userInput;
            // Loop para ler e enviar mensagens do usuário
            while (true) {
                userInput = consoleInput.nextLine(); // Lê mensagem do usuário
                if (userInput.equalsIgnoreCase("/quit")) { // Se for comando de sair
                    break;
                }
                // Verifica se o comando é para enviar um arquivo
                if (userInput.startsWith("/sendfile ")) {
                    // Chama método para enviar arquivo
                    handleSendFile(userInput, out, socket.getOutputStream());
                } else {
                    // Envia mensagem normal ao servidor
                    out.println(userInput);
                }
            }
            // Mensagem de desconexão
            System.out.println("Desconectando...");

        } catch (UnknownHostException e) {
            // Erro de host desconhecido
            System.err.println("Host desconhecido: " + SERVER_HOST);
        } catch (IOException e) {
            // Erro de conexão
            System.err.println("Não foi possível conectar ao servidor. Verifique se ele está online.");
        }
    }

    // Método para enviar arquivo ao servidor
    private static void handleSendFile(String command, PrintWriter out, OutputStream socketOutStream) {
        // Divide o comando em partes
        String[] parts = command.split(" ", 3);
        if (parts.length != 3) {
            // Formato inválido
            System.out.println("Formato inválido. Use: /sendfile <@username/#grupo> <caminho_do_arquivo>");
            return;
        }

        String dest = parts[1]; // Destinatário
        String filepath = parts[2]; // Caminho do arquivo
        File file = new File(filepath); // Cria objeto arquivo

        if (!file.exists()) {
            // Arquivo não encontrado
            System.out.println("Arquivo não encontrado em: " + filepath);
            return;
        }

        try {
            long filesize = file.length(); // Tamanho do arquivo
            String filename = file.getName(); // Nome do arquivo

            // 1. Envia o cabeçalho do arquivo para o servidor
            String fileHeader = "/sendfile " + dest + " " + filename + " " + filesize;
            out.println(fileHeader);

            // 2. Envia o conteúdo binário do arquivo
            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                socketOutStream.write(buffer, 0, bytesRead);
            }
            socketOutStream.flush(); // Garante envio
            fileIn.close(); // Fecha arquivo

            System.out.println("'" + filename + "' enviado para o servidor para roteamento.");

        } catch (IOException e) {
            // Erro ao enviar arquivo
            System.err.println("Falha ao enviar o arquivo: " + e.getMessage());
        }
    }
}

/**
 * Uma classe Runnable para lidar com o recebimento de mensagens do servidor em
 * uma thread separada.
 */
class ReceiverThread implements Runnable {
    // Socket do cliente
    private final Socket socket;
    // Buffer para ler mensagens de texto
    private BufferedReader leitor;
    // Stream para ler dados binários
    private DataInputStream dataLeitor;

    // Construtor
    public ReceiverThread(Socket socket) {
        this.socket = socket;
    }

    // Método principal da thread

    @Override
    public void run() {
        try {
            // Inicializa leitor de texto
            leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Inicializa leitor de dados binários
            dataLeitor = new DataInputStream(socket.getInputStream());

            String mensagem;
            // Loop para receber mensagens do servidor
            while ((mensagem = leitor.readLine()) != null) {
                // Se for comando de recebimento de arquivo
                if (mensagem.startsWith("/recvfile ")) {
                    handleReceiveFile(mensagem);
                } else {
                    // Imprime mensagem recebida
                    System.out.println(mensagem);
                }
            }
        } catch (IOException e) {
            // Mensagem de desconexão
            System.out.println("\nDesconectado do servidor.");
        } finally {
            try {
                // Fecha socket
                socket.close();
            } catch (IOException e) {
                // Ignorar erro no fechamento
            }
            // Encerra o cliente
            System.exit(0); // Força o encerramento do cliente
        }
    }

    // Método para receber arquivo do servidor
    private void handleReceiveFile(String command) {
        try {
            // Protocolo: /recvfile <sender> <filename> <filesize>
            String[] parts = command.split(" ", 4);
            String sender = parts[1]; // Remetente
            String filename = parts[2]; // Nome do arquivo
            long filesize = Long.parseLong(parts[3]); // Tamanho do arquivo

            String downloadDir = "downloads_java";
            new File(downloadDir).mkdirs(); // Garante que o diretório de downloads exista

            File file = new File(downloadDir, "recebido_de_" + sender + "_" + filename); // Arquivo destino
            FileOutputStream fileOut = new FileOutputStream(file);

            System.out.println("\n[ARQUIVO] Recebendo '" + filename + "' (" + filesize + " bytes) de " + sender + ".");
            System.out.println("Salvando em: " + file.getAbsolutePath());

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;

            // Lê os bytes do arquivo do stream de entrada
            while (totalRead < filesize && (bytesRead = dataIn.read(buffer, 0,
                    (int) Math.min(buffer.length, filesize - totalRead))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            fileOut.close(); // Fecha arquivo
            System.out.println("\n[ARQUIVO] '" + filename + "' recebido e salvo com sucesso!");

        } catch (IOException | NumberFormatException e) {
            // Erro ao receber arquivo
            System.err.println("[ERRO] Falha ao receber o arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
