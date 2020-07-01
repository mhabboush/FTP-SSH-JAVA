import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class ServerSideSocket {
    private ServerSocket server;
    private int counter = 1;

    public ServerSideSocket() throws Exception {
        this.server = new ServerSocket(0, 1, InetAddress.getLocalHost());
        System.out.printf("Server created with IP: %s and port number: %d\n", server.getInetAddress().getHostAddress(),server.getLocalPort());
    }

    public void start() throws Exception {
    	//Infinite loop to start any client request in a separate thread
    	while (true) {
	        new SocketThread(this.server.accept(), counter).start();
	        counter++;
    	}
    }
    
}

class SocketThread extends Thread {
	private Socket client;
	private int id;
    private byte [] key;
    final private static BigInteger serverN = new BigInteger("9817766666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666670379887169999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999874799999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999581325509666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666555969");
    final private static BigInteger serverE = new BigInteger("108325863821096339271743678038510451681434491686906079657751452028093823413763042049400478307697212297074336759629414625703387095643482138958998780828831014524007824415583531030206954680050582278471379200955777743843461145928528982886819578723228303574404090524363314368353836759108436703302189971266712813819");
    final private static BigInteger serverD = new BigInteger("2331784308739811027708595367460144570391701954122036163799845464079461469230890829736762599003503133290634382930948445258517497166454464641521831688815154213425754461140868809251024511331108486332483477024961125899297803026503707597804325618580174704726692309935149964426317742768672034694009521659984369658916924501067683705425070184269300710378243311812959668645545074055974243750414700808471095497124900934407570098491336886394084146039254266941438232105890095137401164798852615303545917452113183264246109010241225944996208296429077939775072458942453035685778464811667994108122941316226982240919193851421773007889965803187");
    final private static BigInteger clientN = new BigInteger("15087901461302145926152661281728621908195308879932886656790145723523545901479279301546123923946190657457479724190879902146613302214570147947970214792592614859326126148392859548570815281970882126593282193327905705727972439239261505972661683928395083995239706395306439906595506639996796819063530219241485952641552797263801984982842534096294028942738269336473602466756226688987654098320320096540762762540094316316093648980980758313646756534089422533409854076075853407629629406962294294071626960069847402735846734289622733622276498498275831163162940495828938716271604715603158491602491156489600489155587587364920253363140696029140027582916026915581");
    final private static BigInteger clientE = new BigInteger("42948203888733747736422008150872910905107294657461240753736810246334441241394452697250182699030306962656712954550183172228697591613259822902332009848486556720447815389405608632125488844293073690633783076371511168131431290522640540022609769584685139282357998323782838579729377059946292654453139204131770904231");
    final private static BigInteger g = new BigInteger("2");
    final private static BigInteger m = new BigInteger("32317006071311007300338913926423828248817941241140239112842009751400741706634354222619689417363569347117901737909704191754605873209195028853758986185622153212175412514901774520270235796078236248884246189477587641105928646099411723245426622522193230540919037680524235519125679715870117001058055877651038861847280257976054903569732561526167081339361799541336476559160368317896729073178384589680639671900977202194168647225871031411336429319536193471636533209717077448227988588565369208645296636077250268955505928362751121174096972998068410554359584866583291642136218231078990999448652468262416972035911852507045361090559");
	
    public SocketThread(Socket client, int counter) {
		this.client = client;
		id = counter;
	}
	@Override
	public void run() {
		System.out.println("New connection from " + client.getInetAddress().getHostAddress() + ", Client ID: "+id);
		boolean auth = false;
		try {
			auth = authenticate();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (auth) 
			try {
	        BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
	        int type;
	        while (true) {
	            type = bis.read();
	            if (type == -1)
	            	break;
				byte[] filePath = new byte[bis.read()];
				bis.read(filePath);
				String fileName = new String(filePath, StandardCharsets.US_ASCII);
	            if (type==0) {
	            	int fileSize = 0; 	
	        		fileSize|= (bis.read() << 24);
	        		fileSize|= (bis.read() << 16);
	        		fileSize|= (bis.read() << 8);
	        		fileSize|= (bis.read());
	        		// Phase 2 code, decryption of the file
	    			byte[] IV = new byte[16];
	                bis.read(IV);
	    			byte[] encData = new byte[(fileSize/16+1) * 16]; 
	    			bis.read(encData);
	        		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        		c.init(Cipher.DECRYPT_MODE, getKey(), new IvParameterSpec(IV));
	        		encData = c.doFinal(encData);
	        		// end of phase 2
	                File file = new File(fileName);
	                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
	                bos.write(encData);
	                bos.close();
	                System.out.println("Received file \"" + fileName+"\" with size "+ getSize((int) file.length())+" from client #"+id);
	            } else {
		           	try {
	                	File file = new File(fileName);
	            		BufferedInputStream fb = new BufferedInputStream(new FileInputStream(file));
	                	System.out.println("Request for file \""+fileName+"\" from client #"+id);
	                    byte[] fileSize = toByteArray((int) file.length());
	                    byte[] fileArray = new byte[(int) file.length()];
	                    fb.read(fileArray, 0, fileArray.length);
	                    fb.close();
	                    // Phase 2 code, encryption of the file
	            		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	            		c.init(Cipher.ENCRYPT_MODE, getKey(), getIV());
	            		fileArray = c.doFinal(fileArray);
	            		// end of phase 2 code
	                    client.getOutputStream().write(1);
	                    client.getOutputStream().write(fileSize);
	                    client.getOutputStream().write(c.getIV()); // Phase 2 code
	                    client.getOutputStream().write(fileArray);
	                	System.out.println("Sent " + getSize(fileArray.length+21) + " to client #"+id);
            	} catch (FileNotFoundException e) {
                    client.getOutputStream().write(0);
                	System.out.println("Request for non-existent file \""+fileName+"\" was dismissed from client #"+id);
            	}
            }
        }
        client.close();
        key = null;
        System.out.println("Connection with client #"+id+" terminated");
		} catch (Exception e) {
			System.out.println("Client "+id+" abruptly closed the connection");
	        key = null;
		}
		else
	        System.out.println("Connection with client #"+id+" terminated");
	}
    //Phase 3
	private boolean authenticate() throws Exception  {
        BufferedInputStream bis = new BufferedInputStream(client.getInputStream());

        //read client parameters
        byte [] Ra = new byte[32];
        bis.read(Ra);
    	int gamSize = 0; 	
    	gamSize|= (bis.read() << 24);
    	gamSize|= (bis.read() << 16);
    	gamSize|= (bis.read() << 8);
    	gamSize|= (bis.read());
		byte[] gamArray = new byte[gamSize]; 
		bis.read(gamArray);        
        BigInteger gam = new BigInteger(1,gamArray);

		//Send server parameters
        SecureRandom bGen = SecureRandom.getInstance("SHA1PRNG", "SUN");
        byte [] B = new byte[256];
        bGen.nextBytes(B);
        BigInteger expB = new BigInteger(1,B);
        byte [] Rb = new byte[32];
        bGen.nextBytes(Rb);
        BigInteger gbm = g.modPow(expB, m);
        byte [] gbmArray = gbm.toByteArray();
        byte [] gbmSize = toByteArray(gbmArray.length);
        BigInteger gabm =  gam.modPow(expB, m);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		String message = this.client.getInetAddress().getHostAddress() +  InetAddress.getLocalHost().getHostAddress() + new BigInteger(1,Ra).toString() + new BigInteger(1,Rb).toString() + gam.toString() + gbm.toString() + gabm.toString();
		byte[] hash = digest.digest(message.getBytes());
		byte [] Sb = new BigInteger(1, (new String(hash)+InetAddress.getLocalHost().getHostAddress()).getBytes()).modPow(serverD, serverN).toByteArray();
		byte [] SbSize = toByteArray(Sb.length);
        client.getOutputStream().write(Rb);
        client.getOutputStream().write(gbmSize);
        client.getOutputStream().write(gbmArray);  
		client.getOutputStream().write(SbSize);
		client.getOutputStream().write(Sb);
		B = null; expB = null; bGen = null;
    	
        //Wait for authentication confirmation from client
		int resp = bis.read();
        if(resp==0) {
    		System.out.println("The client denied authentication, terminating connection...");
			Thread.sleep(3 * 1000);
			client.close();
			return false;
        }

		//Authenticate Client
		key = digest.digest(gabm.toByteArray());
		int encAddressWithSaSize = 0;
		encAddressWithSaSize|= (bis.read() << 24);
		encAddressWithSaSize|= (bis.read() << 16);	
		encAddressWithSaSize|= (bis.read() << 8);
		encAddressWithSaSize|= (bis.read());
		byte[] IV = new byte[16];
        bis.read(IV);
		byte[] encAddressWithSa = new byte[encAddressWithSaSize];
		bis.read(encAddressWithSa);
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.DECRYPT_MODE, getKey(), new IvParameterSpec(IV));
		byte[] addressWithSa = c.doFinal(encAddressWithSa);
		byte[] address = this.client.getInetAddress().getHostAddress().getBytes();
		byte[] encSa = new byte[addressWithSa.length - address.length];
		for (int i = 0 ; i<encSa.length ; i++)
			encSa[i] = addressWithSa[i+address.length];
		byte [] decSa = new BigInteger(1,encSa).modPow(clientE, clientN).toByteArray();
		byte [] generatedSa = new byte[hash.length+address.length];
		for (int i = 0 ; i<hash.length ; i++)
			generatedSa[i] = hash[i];
		for (int i = 0 ; i<address.length ; i++)
			generatedSa[i+hash.length] = address[i];
		boolean match = true;
		for (int i = 0 ; i<address.length && match; i++)
			if (addressWithSa[i] != address[i])
				match = false;
		int shift = 0;
		if (decSa[0] == 0)
			shift = 1;
		for (int i = 0 ; i<generatedSa.length && match; i++)
			if (decSa[i+shift] != generatedSa[i])
				match = false;
        client.getOutputStream().write(match?1:0);
		if (!match) {
			System.out.println("The client couldn't be authenticated, terminating connection...");
			Thread.sleep(3 * 1000);
			client.close();
			key = null;
			return false;
		}
		System.out.println("The client has been authenticated.");
		return true;
	}
	private byte[] toByteArray(int value) {
	    return new byte[] {
	            (byte)(value >> 24),
	            (byte)(value >> 16),
	            (byte)(value >> 8),
	            (byte)value};
	}
	
	private String getSize(int size) {
		if (size>=1048576)
			return String.format("%.2f MB", (double)size/(1048576));
		else if (size>=1024)
			return String.format("%.2f KB", (double)size/(1024));
		return size +" bytes";
	}
	// Phase 2 method, generate secure IV
	private static IvParameterSpec getIV() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom IVgen = SecureRandom.getInstance("SHA1PRNG", "SUN");
        byte [] IV = new byte[16];
        IVgen.nextBytes(IV);
        return new IvParameterSpec(IV);
	}
	// Phase 2 method, generate key from a string
	private SecretKeySpec getKey() {
	    return new SecretKeySpec(key, "AES");
	}
	private void printArray(byte [] array) {
		for (byte b : array) {
			System.out.print(b&0xff);
			System.out.print(", ");
		}
		System.out.println();
	}



}