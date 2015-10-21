package node.client;

import index.server.Assign;
import index.server.IndexingServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import node.Peer;
import util.DistributedHashtable;
import util.Util;

public class Client extends Thread {

	private Peer peer;
	private static ArrayList<String> peerList;
	private static ArrayList<String> serverList;
	private ArrayList<Socket> socketList;

	public Client(Peer peer) {
		this.peer = peer;
	}

	// getters
	public Peer getPeer() {
		return this.peer;
	}

	public ArrayList<String> getPeerList() {
		return peerList;
	}

	public ArrayList<Socket> getSocketList() {
		return this.socketList;
	}

	// setters
	public void setPeer(Peer peer) {
		this.peer = peer;
	}

	public void setPeerlist() {
		try {
			peerList = DistributedHashtable.readConfigFile("peers");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setPeerList(ArrayList<String> peerList2) {
		peerList = peerList2;
	}

	public void setServerlist() {
		try {
			serverList = DistributedHashtable.readConfigFile("servers");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setServerList(ArrayList<String> serverList2) {
		serverList = serverList2;
	}

	public void setSocketList(ArrayList<Socket> socketList) {
		this.socketList = socketList;
	}

	// put
	public Boolean registry() throws Exception {

		int pId;
		Socket socket;
		boolean ack = false;
		for (String fileName : peer.getFileNames()) {
			pId = DistributedHashtable.hash(fileName, serverList.size());
			socket = socketList.get(pId);

			// synchronized(socket){
			DataOutputStream dOut = new DataOutputStream(
					socket.getOutputStream());
			// put option
			dOut.writeByte(0);
			dOut.flush();

			// fileName, peer
			dOut.writeUTF(fileName);
			dOut.flush();
			dOut.writeUTF(peer.toString());
			dOut.flush();

			DataInputStream dIn = new DataInputStream(socket.getInputStream());
			ack = dIn.readBoolean();
			if (ack == false)
				break;
			// }
		}

		return ack;
	}

	// get
	public ArrayList<String> search(String fileName) throws IOException {

		int pId = DistributedHashtable.hash(fileName, serverList.size());
		Socket socket = socketList.get(pId);

		ArrayList<String> resultList = new ArrayList<String>();
		int numFiles;
		// synchronized(socket){
		DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

		// lookup option
		dOut.writeByte(1);
		dOut.flush();

		// key
		dOut.writeUTF(fileName);
		dOut.flush();

		DataInputStream dIn = new DataInputStream(socket.getInputStream());
		numFiles = dIn.readInt();
		for (int i = 0; i < numFiles; i++) {
			resultList.add(dIn.readUTF());
		}
		// }

		return resultList;
	}
	
	public void download(String fileName, int peerId, String peer) throws Exception {
		String [] peerAddress = peer.split(":");
		//TODO: create a socket list for the peers too
		Socket socket = new Socket(peerAddress[1], Integer.parseInt(peerAddress[2]));
    	DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
    	dOut.writeUTF(fileName);
        InputStream in = socket.getInputStream();
        
        String folder = "downloads-peer" + peerId + "/";
        File f = new File(folder);
        Boolean created = false;
        if (!f.exists()){
        	try {
        		created = f.mkdir();
        	}catch (Exception e){
        		System.out.println("Couldn't create the folder, the file will be saved in the current directory!");
        	}
        }else {
        	created = true;
        }
        
        //if(i != -1) fileName = fileName + i;
        
        OutputStream out = (created) ? new FileOutputStream(f.toString() + "/" + fileName) : new FileOutputStream(fileName);
        Util.copy(in, out);
        System.out.println("File " + fileName + " received from peer " + peerAddress[0] + ":" + peerAddress[1]);
	}

	public boolean startIndexingServer(int port) {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(port);
			// start server
			IndexingServer server = new IndexingServer(serverSocket);
			server.start();

			// start assign
			Assign assign = new Assign(server);
			assign.start();
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}

		return true;
	}

	public void userInterface() {
		int option, downopt;
		String key = null;
		Boolean result = false;
		ArrayList<String> value;

		System.out.println("Runnning as Peer " + peer.getPeerId());

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("\n\nSelect the option:");
			System.out.println("\t1 - Registry");
			System.out.println("\t2 - Search");
			System.out.println("\t3 - Download");

			try {
				option = scanner.nextInt();
				if (option == 1) {
					try {
						result = registry();
					} catch (Exception e) {
						System.out.println("Couldn't registry in the system.");
					}

					System.out.println(result ? "Registered!"
							: "Not registered.");

				} else if (option == 2) {

					System.out.print("File Name: ");
					key = scanner.next();

					try {
						value = search(key);
					} catch (Exception e) {
						System.out
								.println("Something went wrong and it couldn'd find the value.");
						continue;
					}
					if (value.size() == 0) {
						System.out.println("File not found in the system.");
						continue;
					}
					for (String s : value) {
						System.out.println(s);
					}

				} else if (option == 3) {

					System.out.print("File Name: ");
					key = scanner.next();

					try {
						value = search(key);
					} catch (Exception e) {
						System.out
								.println("Something went wrong and it couldn'd find the value.");
						continue;
					}
					if (value.size() == 0) {
						System.out.println("File not found in the system.");
						continue;
					}
					for (int i = 1; i <= value.size(); i++) {
						System.out.println("\t" + i + " - " + value.get(i-1));
					}
					while(true){
						System.out.print("Peer: ");
						downopt = scanner.nextInt();
						if(downopt < 0 || downopt > value.size())
							continue;
						download(key,downopt-1,value.get(downopt-1));
						break;
					}

				} else {
					System.out.println("Option not valid");
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Oops, something went wrong. Closing it!");
				break;
			}
		}
		scanner.close();
		return;
	}

	public static void main(String[] args) throws IOException {

		peerList = DistributedHashtable.readConfigFile("peers");

		serverList = DistributedHashtable.readConfigFile("servers");

		if (args.length < 1) {
			// System.out.println("Usage: java -jar build/Client.jar <PeerId> <Address> <Port> <Folder>");
			System.out.println("Usage: java -jar build/Client.jar <PeerId>");
			return;
		}

		int id = 0;
		try {
			id = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Put a valid Id");
			return;
		}

		if (id > peerList.size()) {
			System.out
					.println("Peer Id shouldn't be greater than the number provided in the config file!");
			return;
		}

		// String address = args[1];

		/*
		 * int port = 0; try { port = Integer.parseInt(args[2]); } catch
		 * (Exception e) { System.out.println("Put a valid port number");
		 * return; }
		 */

		String[] peerAddress;
		peerAddress = peerList.get(id).split(":");
		String address = peerAddress[0];
		int port = Integer.parseInt(peerAddress[1]);

		// String dir = args[3];
		String dir = peerAddress[2];
		File folder = new File(dir);

		if (!folder.isDirectory()) {
			System.out.println("Put a valid directory name");
			return;
		}

		ArrayList<String> fileNames = Util.listFilesForFolder(folder);
		final Peer peer = new Peer(id, address, port, dir, fileNames,
				fileNames.size());

		new Thread() {
			public void run() {
				try {
					peer.server();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		new Thread() {
			public void run() {
				try {
					peer.assign();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		String[] serverAddress;

		ArrayList<Socket> socketList = new ArrayList<Socket>();

		Client c = new Client(peer);

		// checking if all are open
		for (id = 0; id < serverList.size(); id++) {
			serverAddress = serverList.get(id).split(":");
			address = serverAddress[0];
			port = Integer.parseInt(serverAddress[1]);

			try {
				System.out.println("Testing connection to server " + address
						+ ":" + port);
				Socket s = new Socket(address, port);
				socketList.add(s);
				System.out.println("Server " + address + ":" + port
						+ " is running.");
			} catch (Exception e) {
				// System.out.println("Not connected to server " + address + ":"
				// + port);
				// System.out.println("Trying again");
				id--;
				port--;
			}
		}
		c.setSocketList(socketList);
		c.userInterface();
	}

}
