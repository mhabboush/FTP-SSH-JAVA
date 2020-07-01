import java.net.*;
import java.util.Scanner;

class Project {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int mode = -1;
        boolean fail = true;
        // Read user mode with error handling
        while (fail) {
            try {
                System.out.println("Please specify mode of operation: [0 for server, 1 for client]");
                mode = sc.nextInt();
                if (mode != 0 & mode != 1) {
                    System.out.println("Error: You have chosen a wrong choice.");
                    fail = true;
                } else
                    fail = false;
            } catch (Exception e) {
                fail = true;
                sc.nextLine();
                System.out.println("Error: You have entered something incorrect.");
            }
        }
        try {
            if (mode == 0)
                createServer();
            else
                createClient();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }

    static void createServer() {
    	try {
    	//Create a server socket and start it
        ServerSideSocket server = new ServerSideSocket();
        server.start();
    	} catch (Exception e) {
    		System.out.println("A server couldn't be established, press enter to exit");
    		Scanner sc = new Scanner(System.in);
    		sc.nextLine();
    		sc.close();
    		System.exit(1);
    	}
    }

    static void createClient() {
    	boolean exist = false;
        Scanner sc = new Scanner(System.in);
        //Initialize client socket with error handling for correct IP & port
        ClientSideSocket client = null;
    	while (!exist) {
    		try {
		        System.out.println("Please specify a server to connect to:");
		        String serverIP = sc.nextLine();
		        System.out.println("Please specify a port number:");
		        int serverPort = sc.nextInt();
		        client = new ClientSideSocket(InetAddress.getByName(serverIP.trim()), serverPort);
		        exist = true; 
	        } catch (Exception e) {
	        	sc.nextLine();
	    		System.out.println("Connection to server couldn't be established.\nTo try again enter 1, or enter to exit:");
		        String rs = sc.nextLine();
		        if (!(rs.length() == 1 && rs.charAt(0) == '1')) {
		        	sc.close();
		        	System.exit(1);
		        }
	        }
    	}
    	//Infinite loop for client operations with error handling, only command 3 exits the loop
        while (true) {
        	try {
	            System.out.println("-------------------------------------------------------------");
	            System.out.println("Please choose a command:\n1- GET\n2- PUT\n3- QUIT\n");
	            int command = sc.nextInt();
	            if (command == 1 | command == 2)  {
	                sc.nextLine();
	                System.out.println("Please enter the file name:");
	                String filePath = sc.nextLine();
	                if (command == 1)
	                	client.get(filePath);
	                else
	                	client.put(filePath);
	            }
	            else if (command == 3){
	            	client.close();
	            	break;
	            }
	            else 
	                throw new Exception();
	     }
         catch (Exception e) {
             System.out.println("Invalid choice.");
             sc.nextLine();
             }
        }
        sc.close();
    }
}