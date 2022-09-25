
import java.io.*;
/*
 * Kelas ini mendefinisikan berbagai jenis pesan yang akan dipertukarkan antara
 * Klien dan Server.
 * Saat berbicara dari Klien Java ke Server Java jauh lebih mudah untuk melewatkan objek Java, tidak
 * perlu menghitung byte atau menunggu umpan baris di akhir bingkai
 */

public class ChatMessage implements Serializable {

	// Berbagai jenis pesan yang dikirim oleh Klien
	// WHOISIN untuk menerima daftar pengguna yang terhubung
	// PESAN pesan teks biasa
	// LOGOUT untuk memutuskan sambungan dari Server
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;
	private int type;
	private String message;
	
	// constructor
	ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	int getType() {
		return type;
	}

	String getMessage() {
		return message;
	}
}
