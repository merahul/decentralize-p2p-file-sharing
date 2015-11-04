package bench;

import node.client.Client;

public class RegistryThread extends Thread{
	private Client client;
	private int operations;

	public RegistryThread(Client client, int operations){
		this.client = client;
		this.operations = operations;
	}

	public void run(){

		for(int i = 0; i < operations; i++){
			try{
				client.registry(false);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
