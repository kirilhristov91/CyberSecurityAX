import java.util.Scanner;

public class Main {

    public static void main(String[] args){
    	Scanner sc = new Scanner(System.in);
    	Steg s = new Steg();
    	boolean stop = false;
    	while(!stop){
	    	
    		System.out.println("-------Menu------");
        	System.out.println("  1. Encode message in an image.");
        	System.out.println("  2. Decode message from an image.");
        	System.out.println("  3. Exit.");
        	System.out.print(">> ");
        	
        	int choice = sc.nextInt();
    		switch(choice){
	    		case 1:
	    			System.out.println("\nEnter the name of the image file you want to encode a message in:");
	    			System.out.print(">> ");
	    			String cover_filename = sc.next();
	    			
	    			sc.nextLine();
	    			
	    			System.out.println("Enter the message you want to encode:");
	    			System.out.print(">> ");
	    			String payload = sc.nextLine();
	    			
	    			String stegoImage = s.hideString(payload, cover_filename);
	    			if(stegoImage.equals("Fail")) System.out.println("Fail to encode the message in the specified file!");
	    			else System.out.println("\nThe message was encoded in file: " + stegoImage + "\n");
	    			break;
	    		case 2:	
	    			System.out.println("\nEnter the name of the image file you want to decode:");
	    			System.out.print(">> ");
	    			String stego_image = sc.next();
	    			
	    			String decodedMessage = s.extractString(stego_image);
	    			if(decodedMessage.equals("Fail")) System.out.println("Failed to decode a messag from the specified file!");
	    			else System.out.println("\nThe decoded message is: " + decodedMessage + "\n");
	    			break;
	    		case 3:
	    			stop = true;
	    			System.exit(0);
	    			break;
	    		default:
	    			System.out.println("\nPlease choose a number from 1 to 3 as specified in the menu!");
	    			break;
	    	}
    		
    	}
    	sc.close();
    } 
}
