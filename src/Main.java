import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

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

        final ReadWriteLock lockConnectedClients = new ReentrantReadWriteLock();

        final Lock lockMessageDb = new ReentrantLock();

        final Condition conditionMessageDb = lockMessageDb.newCondition();

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
                try {
                    // lock connectClients
                    lockConnectedClients.writeLock().lock();
                    connectedClients.put(client.id, client);
                }
                finally {
                    // unlock connectedClients
                    lockConnectedClients.writeLock().unlock();
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
            try {
                // lock messageDb
                lockMessageDb.lock();
                List<Message> messages = messageDb.computeIfAbsent(receiver, key -> new ArrayList<>());
                Message msg = new Message(sender, message);
                messages.add(msg);
                // notify messageDb is readable
                conditionMessageDb.signal();
            }
            finally {
                // unlock messageDb
                lockMessageDb.unlock();
            }
        }

        void pushPendingMessages() {
            while (true) {
                // wait till there is any pending message
                try {
                    lockMessageDb.lock();
                    conditionMessageDb.await();
                }
                catch (InterruptedException ex) { ex.printStackTrace(); }
                finally {
                    lockMessageDb.unlock();
                }

                List<Integer> ids;
                // lock connectedClients
                try {
                    lockConnectedClients.readLock().lock();
                    ids = connectedClients.keySet().stream().toList();
                }
                finally {
                    // unlock connectedClients
                    lockConnectedClients.readLock().unlock();
                }

                for (int id : ids) {
                    List<Message> messages;
                    // lock messageDb
                    try {
                        lockMessageDb.lock();
                        messages = messageDb.remove(id);
                    }
                    finally {
                        // unlock messageDb
                        lockMessageDb.unlock();
                    }

                    if (null == messages) {
                        continue;
                    }

                    Client client;
                    // lock connectedClients
                    try {
                        lockConnectedClients.readLock().lock();
                        client = connectedClients.get(id);
                    }
                    finally {
                        // unlock connectedClient
                        lockConnectedClients.readLock().unlock();
                    }

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
