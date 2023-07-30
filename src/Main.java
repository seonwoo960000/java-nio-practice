import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Select either one of the server or client to run the application.");
        // createServer();
        // startClient();
    }

    static void createServer() throws Exception {
        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(8080);
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(address);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                SelectionKey k = i.next();
                if ((k.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    System.out.println("New TCP Connection");

                    i.remove();

                    // retrieve the SocketChannel for the new connection
                    // use the same selector so now we can handle incoming data events on the same event loop
                    ServerSocketChannel ssc = (ServerSocketChannel) k.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                } else if ((k.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    // new data is available
                    i.remove();

                    SocketChannel sc = (SocketChannel) k.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int bytesRead = sc.read(buffer);

                    if (bytesRead == -1) {
                        System.out.println("TCP connection closed");
                        k.cancel();
                    } else {
                        buffer.flip();
                        byte[] content = new byte[bytesRead];
                        buffer.get(content, 0, bytesRead);
                        System.out.println("Received: \n" + new String(content, StandardCharsets.UTF_8));
                    }
                }
            }
        }
    }

    static void startClient() throws Exception {
        final Selector selector = Selector.open();

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8080));
        socketChannel.configureBlocking(false);

        System.out.println("Scoket connected");

        socketChannel.register(selector, SelectionKey.OP_READ);

        new Thread(() -> {
            while (true) {
                try {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                        SelectionKey k = i.next();

                        if ((k.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            i.remove();
                            SocketChannel sc = (SocketChannel) k.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = sc.read(buffer);

                            if (bytesRead == -1) {
                                System.out.println("TCP connection closed");
                                k.cancel();
                            } else {
                                buffer.flip();
                                byte[] content = new byte[bytesRead];
                                buffer.get(content, 0, bytesRead);

                                System.out.println("Received: \n" + new String(content));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception: " + e);
                }
            }
        }, "selector thread").start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            if (Objects.equals(line, "exit")) break;

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            buffer.put(line.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }

        socketChannel.close();
        System.out.println("Socket channel closed");
    }
}

