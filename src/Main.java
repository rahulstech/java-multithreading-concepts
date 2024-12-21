import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    static Random random = new Random();

    static void delay() throws InterruptedException {
        long delay = random.nextLong(10,500);
        Thread.sleep(delay);
    }

    record Message(int sender, String message) {

        @Override
        public String toString() {
            return "[Client#" + sender + "] - " + message;
        }
    }

    record Client(int id) {

        void sendMessages(Server server) {
            for (int i = 0; i < 3; i++) {
                try {
                    delay();
                    int receiver = server.getRandomClientId(id);
                    server.sendMessage(id, receiver, UUID.randomUUID().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void receiveMessage(List<Message> messages) {
            System.out.println("------ [Messages of Client#" + id + "] ------");
            messages.forEach(System.out::println);
            System.out.println("------------------------------------------");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Client client = (Client) o;
            return id == client.id;
        }
    }

    static class Server {

        final int MAX_CLIENTS = 10;

        int[] clientDb = new int[MAX_CLIENTS];

        Map<Integer, List<Message>> messageDb = new HashMap<>();

        List<Integer> availableClientIds = new ArrayList<>();

        Map<Integer, Client> connectedClients = new HashMap<>();

        ExecutorService executor = Executors.newCachedThreadPool();

        final Object monitorConnectedClients = new Object();

        final Object monitorMessageDb = new Object();

        Server() {
            int id;
            for (int i=0; i<MAX_CLIENTS; i++) {
                id = i+1;
                clientDb[i] = id;
                availableClientIds.add(id);
            }
        }

        public void start() throws Exception {
            executor.execute(this::pushPendingMessages);
            for (int i = 0; i < MAX_CLIENTS; i++) {
                Thread.sleep(100*i);
                Client client = newClient();
                synchronized (monitorConnectedClients) {
                    connectedClients.put(client.id, client);
                }
                serveClient(client);
            }
        }

        void serveClient(Client client) {
            executor.execute(()->{
                try {
                    client.sendMessages(Server.this);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        Client newClient() {
            int size = availableClientIds.size();
            int index = size==1 ? 0 : random.nextInt(size-1);
            int id = availableClientIds.remove(index);
            return new Client(id);
        }

        int getRandomClientId(int exclude) {
            int srcIndex = Arrays.binarySearch(clientDb, exclude);
            int lenBefore = srcIndex+1;
            int lenAfter = clientDb.length - lenBefore;
            int bound = Math.max(lenBefore, lenAfter);
            int index = random.nextInt(bound-1);
            return clientDb[index];
        }

        void sendMessage(int sender, int receiver, String message) {
            // lock messageDb
            synchronized (monitorMessageDb) {
                List<Message> messages = messageDb.computeIfAbsent(receiver, key -> new ArrayList<>());
                Message msg = new Message(sender, message);
                messages.add(msg);
                // notify messageDb is readable
                monitorMessageDb.notify();
                // unlock messageDb
            }
        }

        void pushPendingMessages() {
            while (true) {
                // wait till there is any pending message
                synchronized (monitorMessageDb) {
                    try { monitorMessageDb.wait(); }
                    catch (InterruptedException ex) { ex.printStackTrace(); }
                }

                List<Integer> ids;
                // lock connectedClients
                synchronized (monitorConnectedClients) {
                    ids = connectedClients.keySet().stream().toList();
                }
                // unlock connectedClients
                for (int id : ids) {

                    List<Message> messages;
                    // lock messageDb
                    synchronized (monitorMessageDb) {
                        messages = messageDb.remove(id);
                    }
                    // unlock messageDb

                    if (null == messages) {
                        continue;
                    }

                    Client client;
                    // lock connectedClients
                    synchronized (monitorConnectedClients) {
                        client = connectedClients.get(id);
                    }
                    // unlock connectedClient

                    if (null == client) {
                        continue;
                    }

                    client.receiveMessage(messages);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.start();
    }
}
