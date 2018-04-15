package hu.petrik.chatlib.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * A chat szerveren belül egy kliens kapcsolatot kezel.
 */
class Client {
    private Socket socket;
    private ChatServer server;
    private Object syncRoot = new Object();
    private OutputStreamWriter writer;
    
    private String nickname;
    private static Random random = new Random();
    
    /**
     * Létrehoz egy kliens objektumot a megadott socket-hez.
     * 
     * @param socket A kapcsolat a klienshez.
     * @param server A szerver objektum, amely létrehozta a klienst.
     */
    public Client(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.nickname = "User #" + random.nextLong();
    }

    public String getNickname() {
        return nickname;
    }
    
    /**
     * Fogadja a kliens által küldött üzeneteket, és továbbítja a szervernek.
     * A függvény blokkoló. Csak egyszer hívjuk meg!
     * Ha kivételt dob, feltételezhetjük, hogy a kliens kapcsolat használhatatlan.
     * 
     * @throws IOException 
     */
    public void start() throws IOException {
        writer = new OutputStreamWriter(socket.getOutputStream());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            server.send(nickname + " csatlakozott a szerverhez\n");
            while ((line = reader.readLine()) != null) {
                String[] darabolt = line.split(" ", 2);
                
                if (darabolt.length == 0) {
                    continue;
                }
                
                String parancs = darabolt[0];
                String parameter = "";
                if (darabolt.length != 1) {
                    parameter = darabolt[1];
                }
                
                switch (parancs) {
                    case "/q":
                        socket.close();
                        server.send("* " + nickname + " elhagyta a szobát");
                        return;
                        
                    case "/y":
                        if(line.length() > 3)
                            server.send(nickname + " kiáltja: " + line.substring(3, line.length()).toUpperCase() + "\n");
                        break;
                        
                    case "/nick":
                        String ujNicknev = parameter.trim();
                        if (!ujNicknev.equals("") && !ujNicknev.equals("Admin") && !ujNicknev.equals("SysError")) {
                            if(Character.isUpperCase(ujNicknev.charAt(0))){
                                if(ujNicknev.length() >= 4){
                                    if(!server.isNickNameInUse(ujNicknev)){
                                        server.send("* " + nickname + " új nickneve " + ujNicknev + "\n");
                                        nickname = ujNicknev;
                                    }
                                    else{
                                        send("Ez a nickname már használatban van!\n");
                                    }
                                }
                                else{
                                    send("A nicknév minimum 4 karakterből kell, hogy álljon\n");
                                }                                        
                            }
                            else{
                                send("A nicknévnek nagy betűvel kell kezdődnie!\n");
                            }
                        }
                        else{
                            send("A becenév nem lehet üres, nem lehet Admin és nem lehet SysError\n");
                        }
                        
                        break;
                        
                    default:
                        Date d1 = new Date();
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                        String formattedDate = df.format(d1);
                        server.send(nickname + "["+formattedDate+"]: " + line + "\n");
                        break;
                }
            }
        }
    }
    
    /**
     * Elküldi a megadott üzenetet a kliensnek.
     * A függvény szálbiztos.
     * 
     * @param message
     * @throws IOException 
     */
    void send(String message) throws IOException {
        synchronized(syncRoot) {
            writer.write(message);
            writer.flush();
        }
    }
    
    /**
     * Leállítja a szervert (a hatására a {@link #start()} függvény kivételt dob).
     * A függvény szálbiztos.
     * 
     * @throws IOException 
     */
    public void stop() throws IOException {
        socket.close();
    }
}
