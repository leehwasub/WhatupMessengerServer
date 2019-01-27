package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

import controller.HomeController;

public class Client {

	private Socket socket;
	private String userId;

	public Client(Socket socket) {
		this.socket = socket;
		receive();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	// 클라이언트로부터 메세지를 전달 받는 메소드 입니다.
	public void receive() {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while (true) {
					InputStream in;
					try {
						in = socket.getInputStream();
						DataInputStream dis = new DataInputStream(in);
						System.out.println("[메세지 수신 성공]" + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName());
						String message = dis.readUTF();
						receiveHandler(message);
					} catch (IOException e) {
						System.out.println("[메세지 수신 실패]" + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName());
						break;
					}
				}
			}
		};
		HomeController.threadPool.submit(thread);
	}
	
	public void receiveHandler(String message){
		String[] token = message.split("/");
		System.out.println("서버 -> 클라이언트 : " + message);
		Vector<Client> clients = HomeController.clients;
		if (token[0].equals("message")) {
			for (Client client : HomeController.clients) {
				client.send(message);
			}
		} else if (token[0].equals("addUserList")) {
			clients.get(clients.size() - 1).setUserId(token[1]);
			for (int i = 0; i < clients.size(); i++) {
				System.out.println(clients.get(i).toString());
				clients.get(i).send(message);
			}
			for (int i = 0; i < HomeController.clients.size() - 1; i++) {
				clients.get(clients.size() - 1).send("oldUserList/" + clients.get(i).userId);
			}
		} else if (token[0].equals("deleteUser")) {
			for (int i = 0; i < clients.size(); i++) {
				clients.get(i).send(message);
			}
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).getUserId().equals(token[1])) {
					clients.remove(i);
				}
			}
		} else if (token[0].equals("addAllUsers")) {
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).getUserId().equals(token[1])) {
					for (int j = 0; j < clients.size(); j++) {
						clients.get(i).send("addUserList/" + clients.get(j).userId);
					}
					break;
				}
			}
		} else if(token[0].equals("requestTalk")) {
			String receiver = token[2];
			for(int i = 0; i < clients.size(); i++) {
				if(clients.get(i).getUserId().equals(receiver)) {
					clients.get(i).send(message);
				}
			}
		} else if(token[0].equals("refuseTalk")) {
			String sender = token[1];
			for(int i = 0; i < clients.size(); i++) {
				if(clients.get(i).getUserId().equals(sender)) {
					clients.get(i).send(message);
				}
			}
		}  else if(token[0].equals("acceptTalk")) {
			String sender = token[1];
			String receiver = token[2];
			for(int i = 0; i < clients.size(); i++) {
				if(clients.get(i).getUserId().equals(sender)) {
					clients.get(i).send("acceptTalk/" + receiver);
				} else if(clients.get(i).getUserId().equals(receiver)) {
					clients.get(i).send("acceptTalk/" + sender);
				}
			}
		} else if(token[0].equals("messageTalkAlone")) {
			String sender = token[1];
			String receiver = token[2];
			for(int i = 0; i < clients.size(); i++) {
				if(clients.get(i).getUserId().equals(sender)) {
					clients.get(i).send(message);
				} else if(clients.get(i).getUserId().equals(receiver)) {
					clients.get(i).send(message);
				}
			}
		}
	}

	public void send(String message) {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					DataOutputStream dos = new DataOutputStream(out);
					dos.writeUTF(message);
				} catch (IOException e) {
					try {
						System.out.println("[메세지 송신 오류]" + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName());
						HomeController.clients.remove(Client.this);
						socket.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		HomeController.threadPool.submit(thread);
	}

	@Override
	public String toString() {
		return "Client [socket=" + socket + ", userId=" + userId + "]";
	}

}
