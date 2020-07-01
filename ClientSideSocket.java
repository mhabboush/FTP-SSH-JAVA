import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import java.security.*;

public class ClientSideSocket {
    private Socket socket;
    private byte [] key;
    final private static BigInteger clientN = new BigInteger("15087901461302145926152661281728621908195308879932886656790145723523545901479279301546123923946190657457479724190879902146613302214570147947970214792592614859326126148392859548570815281970882126593282193327905705727972439239261505972661683928395083995239706395306439906595506639996796819063530219241485952641552797263801984982842534096294028942738269336473602466756226688987654098320320096540762762540094316316093648980980758313646756534089422533409854076075853407629629406962294294071626960069847402735846734289622733622276498498275831163162940495828938716271604715603158491602491156489600489155587587364920253363140696029140027582916026915581");
    final private static BigInteger clientE = new BigInteger("42948203888733747736422008150872910905107294657461240753736810246334441241394452697250182699030306962656712954550183172228697591613259822902332009848486556720447815389405608632125488844293073690633783076371511168131431290522640540022609769584685139282357998323782838579729377059946292654453139204131770904231");
    final private static BigInteger clientD = new BigInteger("6712115425168063477678407044464209029198199807463520629171208918692794125051744141008176433309046307251340248490796092288321321342897905144701032772633771231878476443113355872706038152210249223758555006733070483679528647614066970560029114039958675485477779556057780765503236276377379555161932677164350619008169573017004289806308340783188786871275231459038196484366327200062739507300504236919421829515853570153337236949665837436543454182773572894792990627127623541326965143241881966089076572472894493224340064377983138930902772245695249249591885581880292321331176409177691572029964696010096799314395094168892401575973191188990780314926463415771");
    final private static BigInteger serverN = new BigInteger("9817766666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666670379887169999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999874799999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999581325509666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666555969");
    final private static BigInteger serverE = new BigInteger("108325863821096339271743678038510451681434491686906079657751452028093823413763042049400478307697212297074336759629414625703387095643482138958998780828831014524007824415583531030206954680050582278471379200955777743843461145928528982886819578723228303574404090524363314368353836759108436703302189971266712813819");
    final private static BigInteger g = new BigInteger("2");
    final private static BigInteger m = new BigInteger("32317006071311007300338913926423828248817941241140239112842009751400741706634354222619689417363569347117901737909704191754605873209195028853758986185622153212175412514901774520270235796078236248884246189477587641105928646099411723245426622522193230540919037680524235519125679715870117001058055877651038861847280257976054903569732561526167081339361799541336476559160368317896729073178384589680639671900977202194168647225871031411336429319536193471636533209717077448227988588565369208645296636077250268955505928362751121174096972998068410554359584866583291642136218231078990999448652468262416972035911852507045361090559");


    public ClientSideSocket(InetAddress serverAddress, int serverPort) throws Exception {
        this.socket = new Socket(serverAddress, serverPort);
        System.out.println("Connection established to Server: " + socket.getInetAddress());
        authenticate();
    }
    //Phase 3
    private void authenticate() throws Exception {
        BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());
		
        //Send client parameters
        SecureRandom aGen = SecureRandom.getInstance("SHA1PRNG", "SUN");
        byte [] A = new byte[256];
        aGen.nextBytes(A);
        BigInteger expA = new BigInteger(1,A);
        byte [] Ra = new byte[32];
        aGen.nextBytes(Ra);
        BigInteger gam = g.modPow(expA, m);
        byte [] gamArray = gam.toByteArray();
        byte [] gamSize = toByteArray(gamArray.length);
        this.socket.getOutputStream().write(Ra);
        this.socket.getOutputStream().write(gamSize);
        this.socket.getOutputStream().write(gamArray);
        

        //Read server parameters
        byte [] Rb = new byte[32];
        bis.read(Rb);
    	int gbmSize = 0; 	
    	gbmSize|= (bis.read() << 24);
    	gbmSize|= (bis.read() << 16);
    	gbmSize|= (bis.read() << 8);
    	gbmSize|= (bis.read());
		byte[] gbmArray = new byte[gbmSize]; 
		bis.read(gbmArray);        
        BigInteger gbm = new BigInteger(1,gbmArray);
        int SbSize = 0; 	
    	SbSize|= (bis.read() << 24);
    	SbSize|= (bis.read() << 16);
    	SbSize|= (bis.read() << 8);
    	SbSize|= (bis.read());
        byte[] encSb = new byte[SbSize];
		bis.read(encSb);
				
		//Authentica server
		byte [] decSb = new BigInteger(1,encSb).modPow(serverE, serverN).toByteArray();
        BigInteger gabm =  gbm.modPow(expA, m);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		String message = InetAddress.getLocalHost().getHostAddress() + this.socket.getInetAddress().getHostAddress() + new BigInteger(1,Ra).toString() + new BigInteger(1,Rb).toString() + gam.toString() + gbm.toString() + gabm.toString();
		byte[] generatedHash = digest.digest(message.getBytes());
		byte [] generatedSb = new BigInteger(1, (new String(generatedHash)+this.socket.getInetAddress().getHostAddress()).getBytes()).toByteArray();
		boolean match = true;
		for (int i = 0 ; i < generatedSb.length && match; i++)
			if (generatedSb[i] != decSb[i])
				match = false;
		A = null; expA = null; aGen = null; 
        this.socket.getOutputStream().write(match?1:0);
		if (!match) {
			System.out.println("The server couldn't be authenticated, terminating connection...");
			Thread.sleep(3 * 1000);
			close();
			System.exit(1);
		}
		
		//Send step 3 parameters
		byte [] address = InetAddress.getLocalHost().getHostAddress().getBytes();
		//byte [] decSa = new BigInteger(1, (new String(generatedHash)+InetAddress.getLocalHost().getHostAddress()).getBytes()).toByteArray();
		byte [] decSa = new byte[generatedHash.length+address.length];
		for (int i = 0 ; i<generatedHash.length ; i++)
			decSa[i] = generatedHash[i];
		for (int i = 0 ; i<address.length ; i++)
			decSa[i+generatedHash.length] = address[i];
		byte [] encSa = new BigInteger(1,decSa).modPow(clientD, clientN).toByteArray();
		byte [] addressWithSa = new byte[encSa.length + address.length];	
		for (int i = 0 ; i < address.length ; i++)
			addressWithSa[i] = address[i];
		for (int i = address.length ; i < addressWithSa.length ; i++)
			addressWithSa[i] = encSa[i-address.length];
		key = digest.digest(gabm.toByteArray());
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, getKey(), getIV());
		byte [] encAddressWithSa = c.doFinal(addressWithSa);
		byte [] encAddressWithSaSize = toByteArray(encAddressWithSa.length);
        this.socket.getOutputStream().write(encAddressWithSaSize);
        this.socket.getOutputStream().write(c.getIV());
        this.socket.getOutputStream().write(encAddressWithSa); 
        
        //Wait for authentication confirmation from server
		int resp = bis.read();
        if(resp==0) {
    		System.out.println("The server denied authentication, terminating connection...");
			Thread.sleep(3 * 1000);
			close();
			key = null;
			System.exit(1);
        }
		System.out.println("The server has been authenticated.");
     	     	
	}

	public void get(String filePath) throws InterruptedException {
    	try {
	        byte[] pathName = (filePath).getBytes(StandardCharsets.US_ASCII);
	        this.socket.getOutputStream().write(1);
	        this.socket.getOutputStream().write(pathName.length);
	        this.socket.getOutputStream().write(pathName);
	        System.out.println("Request sent");
	        BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream());
	        int response = bis.read();
	        if (response == 0)
	            System.out.println("Error: File doesn't exist on the server.");
	        else {
		    	int fileSize = 0; 	
				fileSize|= (bis.read() << 24);
				fileSize|= (bis.read() << 16);
				fileSize|= (bis.read() << 8);
				fileSize|= (bis.read());
        		// Phase 2 code, decryption of the file
				byte [] IV = new byte[16];
				bis.read(IV);
				byte [] encFile = new byte[(fileSize/16+1) * 16];
				int size = encFile.length;
				bis.read(encFile);
	    		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    		c.init(Cipher.DECRYPT_MODE, getKey(), new IvParameterSpec(IV));
	    		encFile = c.doFinal(encFile);
        		// end of phase 2
		        File file = new File(filePath);
		        BufferedOutputStream fb = new BufferedOutputStream(new FileOutputStream(file));
		        fb.write(encFile);
		        fb.close();
		        System.out.println("Received " + getSize(size+21) + " from server.");
	        }
    	} catch (Exception e) {
    		System.out.println("Server abruptly closed the connection. Terminating connection...");
			Thread.sleep(3 * 1000);
			close();
    		System.exit(1);
    	}	
    }
        
    public void put(String filePath) throws InterruptedException {
    	try {
	        File file = new File(filePath);
	        byte[] pathName = (filePath).getBytes(StandardCharsets.US_ASCII);
	        byte[] fileSize = toByteArray((int) file.length());
	        byte[] fileArray = new byte[(int) file.length()];
	        BufferedInputStream fb = new BufferedInputStream(new FileInputStream(file));
	        fb.read(fileArray);
	        fb.close();
            // Phase 2 code, encryption of the file
    		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
    		c.init(Cipher.ENCRYPT_MODE, getKey(), getIV());
    		fileArray = c.doFinal(fileArray);
    		// end of phase 2 code
    		
	        this.socket.getOutputStream().write(0);
	        this.socket.getOutputStream().write(pathName.length);
	        this.socket.getOutputStream().write(pathName);
	        this.socket.getOutputStream().write(fileSize);
	        this.socket.getOutputStream().write(c.getIV()); //phase 2 code
	        this.socket.getOutputStream().write(fileArray); 
	        System.out.println("Sent " + getSize(fileArray.length+pathName.length+22) + " to server.");
    	}catch (FileNotFoundException nf) {
	        System.out.println("Error: File doesn't exist.");
    	}
    	catch (Exception e) {
    		System.out.println("Server abruptly closed the connection. Terminating connection...");
			Thread.sleep(3 * 1000);
			close();
    		System.exit(1);
    	}
    }
    
	public void close() {
		try {
			this.socket.close();
			key = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String bytesToHex(byte[] bytes) {
		final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
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
	private IvParameterSpec getIV() throws NoSuchAlgorithmException, NoSuchProviderException {
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