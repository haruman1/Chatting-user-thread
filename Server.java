
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

// the server that can be run as a console
public class Server {
	// ID unik untuk setiap koneksi
	private static int uniqueId;
	// sebuah ArrayList untuk menyimpan daftar Klien
	private ArrayList<ClientThread> al;
	// untuk menampilkan waktu
	private SimpleDateFormat sdf;
	// nomor port untuk mendengarkan koneksi 
	private int port;
	// untuk memeriksa apakah server sedang berjalan
	private boolean keepGoing;
	// pemberitahuan
	private String notif = " *** ";
	
	//konstruktor yang menerima port untuk mendengarkan koneksi sebagai parameter
	
	public Server(int port) {
		// port yang akan dipake
		this.port = port;
		// untuk menampilkan HH:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// sebuah ArrayList untuk menyimpan daftar Klien
		al = new ArrayList<ClientThread>();
	}
	
	public void start() {
		keepGoing = true;
		//buat server soket dan tunggu permintaan koneksi
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// looping secara terus menerus untuk menunggu koneksi (sampai server aktif)
			while(keepGoing) 
			{
				display("Server waiting for Clients on port " + port + ".");
				
				// terima koneksi jika diminta dari klien
				Socket socket = serverSocket.accept();
				// istirahat jika server berhenti
				if(!keepGoing)
					break;
				// jika klien terhubung, buat utasnya
				ClientThread t = new ClientThread(socket);
				//tambahkan klien ini ke daftar array
				al.add(t);
				
				t.start();
			}
			// coba hentikan server
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					// tutup semua aliran data dan soket
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}
	
	// untuk menghentikan server 
	protected void stop() {
		keepGoing = false;
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
		}
	}
	
	// Menampilkan acara ke konsol
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// untuk menyiarkan pesan ke semua Klien
	private synchronized boolean broadcast(String message) {
		
		// tambahkan stempel waktu ke pesan
		String time = sdf.format(new Date());
		
		// untuk memeriksa apakah pesan bersifat pribadi yaitu pesan klien ke klien
		String[] w = message.split(" ",3);
		
		boolean isPrivate = false;
		if(w[1].charAt(0)=='@') 
			isPrivate=true;
		
		
		// jika pesan pribadi, kirim pesan ke nama pengguna yang disebutkan saja
		if(isPrivate==true)
		{
			String tocheck=w[1].substring(1, w[1].length());
			
			message=w[0]+w[2];
			String messageLf = time + " " + message + "\n";
			boolean found=false;
			// kita mengulang dalam urutan terbalik untuk menemukan nama pengguna yang disebutkan
			for(int y=al.size(); --y>=0;)
			{
				ClientThread ct1=al.get(y);
				String check=ct1.getUsername();
				if(check.equals(tocheck))
				{
					
					// coba tulis ke Klien jika gagal hapus dari daftar
					if(!ct1.writeMsg(messageLf)) {
						al.remove(y);
						display("Disconnected Client " + ct1.username + " removed from list.");
					}
					// nama pengguna ditemukan dan mengirimkan pesan
					found=true;
					break;
				}
				
				
				
			}
			// pengguna yang disebutkan tidak ditemukan, kembalikan salah
			if(found!=true)
			{
				return false; 
			}
		}
		// jika pesan adalah pesan siaran
		else
		{
			String messageLf = time + " " + message + "\n";
			// tampilkan pesan
			System.out.print(messageLf);
			
			// kita mengulang dalam urutan terbalik jika kita harus menghapus Klien
			// karena telah terputus
			for(int i = al.size(); --i >= 0;) {
				ClientThread ct = al.get(i);
				// coba tulis ke Klien jika gagal hapus dari daftar
				if(!ct.writeMsg(messageLf)) {
					al.remove(i);
					display("Disconnected Client " + ct.username + " removed from list.");
				}
			}
		}
		return true;
		
		
	}

	
	// jika klien mengirim pesan LOGOUT untuk keluar
	synchronized void remove(int id) {
		
		String disconnectedClient = "";
		// pindai daftar array sampai kami menemukan Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// jika ditemukan hapus
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				al.remove(i);
				break;
			}
		}
		broadcast(notif + disconnectedClient + " has left the chat room." + notif);
	}
	
	
		/*
		* Untuk dijalankan sebagai aplikasi konsol
		* > Java Server
		* > Java Server portNumber
		* Jika nomor port tidak ditentukan 1500 digunakan
		*/ 
	public static void main(String[] args) {
		// memulai server pada port 1500 kecuali jika PortNumber ditentukan 
		int portNumber = 1500;
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		// buat objek server dan mulai
		Server server = new Server(portNumber);
		server.start();
	}

	// Satu contoh utas ini akan berjalan untuk setiap klien
	class ClientThread extends Thread {
		// soket untuk menerima pesan dari klien
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// id unik saya (lebih mudah untuk dekoneksi)
		int id;
		// Nama Pengguna Klien
		String username;
		// objek pesan untuk menerima pesan dan jenisnya
		ChatMessage cm;
		// stempel waktu
		String date;

		// Konstruktor
		ClientThread(Socket socket) {
			// sebuah id unik
			id = ++uniqueId;
			this.socket = socket;
			//Membuat kedua Aliran Data
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				
			// baca nama pengguna
				username = (String) sInput.readObject();
				broadcast(notif + username + " has joined the chat room." + notif);
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		// loop tak terbatas untuk membaca dan meneruskan pesan
		public void run() {
			// untuk mengulang sampai LOGOUT
			boolean keepGoing = true;
			while(keepGoing) {
				// membaca String (yang merupakan objek)
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// dapatkan pesan dari objek ChatMessage yang diterima
				String message = cm.getMessage();

				// tindakan berbeda berdasarkan jenis pesan
				switch(cm.getType()) {

				case ChatMessage.MESSAGE:
					boolean confirmation =  broadcast(username + ": " + message);
					if(confirmation==false){
						String msg = notif + "Sorry. No such user exists." + notif;
						writeMsg(msg);
					}
					break;
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// kirim daftar klien aktif
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
				}
			}
			// jika keluar dari loop maka terputus dan hapus dari daftar klien
			remove(id);
			close();
		}
		
		
			// tutup semuanya
		private void close() {
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		// tulis sebuah String ke aliran keluaran Klien
		private boolean writeMsg(String msg) {
			// jika Klien masih terhubung, kirim pesan ke sana
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// tulis pesan ke aliran
			try {
				sOutput.writeObject(msg);
			}
			// jika terjadi kesalahan, jangan digugurkan cukup beri tahu pengguna
			catch(IOException e) {
				display(notif + "Error sending message to " + username + notif);
				display(e.toString());
			}
			return true;
		}
	}
}

