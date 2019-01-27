package controller;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

import application.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;

public class HomeController implements Initializable{
	
    @FXML
    private JFXButton serverStart;

    @FXML
    private JFXTextArea log;

    @FXML
    private JFXTextField ipText;

    @FXML
    private JFXTextField portText;
    
    public static ExecutorService threadPool;
    public static Vector<Client> clients = new Vector<Client>();
    
    private ServerSocket serverSocket;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.setStyle("-fx-text-inner-color: #a0a2ab");
		ipText.setStyle("-fx-text-inner-color: #a0a2ab");
		portText.setStyle("-fx-text-inner-color: #a0a2ab");
		log.setEditable(false);
	}
	
	public void startServer(String IP, int port){
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port));
		} catch(Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						log.appendText("[유저 접속 대기중]\n");
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						System.out.println("[클라이언트 접속] " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName());
						System.out.println(socket.toString());
					}catch(Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
		threadPool = Executors.newCachedThreadPool();
		threadPool.submit(thread);
	}
	
	public void stopServer() {
		try {
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()) {
				Client client = iterator.next();
				client.getSocket().close();
				iterator.remove();
			}
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void serverButtonAction(ActionEvent event) {
		if(ipText.getText().isEmpty() || portText.getText().isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText(null);
			alert.setContentText("please fill the ip and port");
			alert.show();
			return;
		}
		StringTokenizer token = new StringTokenizer(ipText.getText(), ".");
		int cnt = 0;
		while(token.hasMoreTokens()) {
			token.nextToken();
			cnt++;
		}
		if(cnt != 4) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText(null);
			alert.setContentText("please write the correct ip");
			alert.show();
			return;
		}
		if(serverStart.getText().equals("Start")) {
			startServer(ipText.getText(), Integer.parseInt(portText.getText()));
			Platform.runLater(()->{
				String message = String.format("[서버 시작] - ip : " + ipText.getText() + ", port : " + portText.getText() + "\n");
				log.appendText(message);
				serverStart.setText("Stop");
			});
		} else {
			stopServer();
			Platform.runLater(()->{
				String message = String.format("[서버 종료] - ip : " + ipText.getText() + ", port : " + portText.getText() + "\n");
				log.appendText(message);
				serverStart.setText("Start");
			});
		}
	}
    
    
}
