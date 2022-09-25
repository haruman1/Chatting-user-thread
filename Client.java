
import java.net.*;
import java.io.*;
import java.util.*;


//The Client that can be run as a console
public class Client  {
	
	// pemberitahuan
	private String notif = " *** ";

	// untuk I/O
	private ObjectInputStream sInput;		// untuk membaca dari soket
	private ObjectOutputStream sOutput;		// untuk menulis di soket
	private Socket socket;					// objek soket
	
	private String server, username;	// server dan nama pengguna
	private int port;					//port

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	
/*
* Konstruktor untuk mengatur hal-hal di bawah ini
* server: alamat server
* port: nomor port
* nama pengguna: nama pengguna
*/
	
	Client(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	/*
	 * Untuk memulai obrolan
	 */
	public boolean start() {
		// coba sambungkan ke server
		try {
			socket = new Socket(server, port);
		} 
		// penangan pengecualian jika gagal
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		/* Creating both Data Stream */
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// membuat Thread untuk mendengarkan dari server
		new ListenFromServer().start();
		
		// Kirim nama pengguna kami ke server ini adalah satu-satunya pesan yang kami
		// akan dikirim sebagai String. Semua pesan lainnya akan menjadi objek ChatMessage
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// sukses, kami memberi tahu penelepon bahwa itu berhasil
		return true;
	}

	/*
	 * /*
	 * To send a message to the console
	 */
	private void display(String msg) {

		System.out.println(msg);
		
	}
	
	/*
	 * Untuk mengirim pesan ke server
	 */
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * Ketika terjadi kesalahan
	 * Tutup aliran Input/Output dan putuskan sambungan
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {}
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {}
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {}
			
	}
	/*
	* Untuk memulai Klien dalam mode konsol gunakan salah satu dari perintah berikut
	* > klien java
	* > nama pengguna klien java
	* > portNumber nama pengguna klien java
	* > nama pengguna klien java portNumber serverAddress
	* pada prompt konsol
	* Jika portNumber tidak ditentukan 1500 digunakan
	* Jika serverAddress tidak ditentukan "localHost" digunakan
	* Jika nama pengguna tidak ditentukan "Anonim" digunakan
	*/
	public static void main(String[] args) {
		// nilai default jika tidak dimasukkan
		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "Anonymous";
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Enter the username: ");
		userName = scan.nextLine();

		// kasus yang berbeda sesuai dengan panjang argumen.
		switch(args.length) {
			case 3:
				// untuk > javac Nama pengguna klien portNumber serverAddr
				serverAddress = args[2];
			case 2:
				// untuk > javac Nama pengguna klien portNumber
				try {
					portNumber = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
					return;
				}
			case 1: 
				// untuk > nama pengguna klien javac
				userName = args[0];
			case 0:
				// untuk > klien java
				break;
			// jika jumlah argumen tidak valid
			default:
				System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
			return;
		}
		// create the Client object
		Client client = new Client(serverAddress, portNumber, userName);
		// try to connect to the server and return if not connected
		if(!client.start())
			return;
		
		System.out.println("\nHalo.! Selamat datang di ruang obrolan.");
		System.out.println("instruksi:");
		System.out.println("1. Cukup ketik pesan untuk mengirim siaran ke semua klien aktif");
		System.out.println("2. Ketik '@namapengguna<spasi>pesan Anda' tanpa tanda kutip untuk mengirim pesan ke klien yang diinginkan");
		System.out.println("3. Ketik 'ADASIAPA' tanpa tanda kutip untuk melihat daftar klien aktif");
		System.out.println("4. Ketik 'LOGOUT' tanpa tanda kutip untuk logoff dari server");
		
		// loop tak terbatas untuk mendapatkan input dari pengguna
		while(true) {
			System.out.print("> ");
			// membaca pesan dari pengguna
			String msg = scan.nextLine();
			// logout jika pesannya LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				break;
			}
			// pesan untuk memeriksa siapa yang hadir di ruang obrolan
			else if(msg.equalsIgnoreCase("ADASIAPA")) {
				client.sendMessage(new ChatMessage(ChatMessage.ADASIAPA, ""));				
			}
			// pesan teks biasa
			else {
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
			}
		}
		// tutup sumber daya
		scan.close();
		// klien menyelesaikan tugasnya. putuskan klien.
		client.disconnect();	
	}

	 
	 /*
	  * kelas yang menunggu pesan dari server
	  */
	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					// baca pesan dari input datastream
					String msg = (String) sInput.readObject();
					// cetak pesan
					System.out.println(msg);
					System.out.print("> ");
				}
				catch(IOException e) {
					display(notif + "Server has closed the connection: " + e + notif);
					break;
				}
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}

