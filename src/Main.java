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
			System.out.println("  3. Encode file in an image.");
			System.out.println("  4. Decode file from an image.");
        	System.out.println("  5. Exit.");
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
	    			if(decodedMessage.equals("Fail")) System.out.println("Failed to decode a message from the specified file!");
	    			else System.out.println("\nThe decoded message is: " + decodedMessage + "\n");
	    			break;
                case 3:
                    System.out.println("\nEnter the name of the file you want to encode in a picture:");
                    System.out.print(">> ");
                    String filename = sc.next();

                    sc.nextLine();

                    System.out.println("Enter the name of the image file you want to encode the file in:");
                    System.out.print(">> ");
                    String imageFile = sc.nextLine();

                    String imageWithFileEncoded = s.hideFile(filename, imageFile);
                    if(imageWithFileEncoded.equals("Fail")) System.out.println("Fail to encode the file in the specified image file!");
                    else System.out.println("\nThe file was encoded in image file: " + imageWithFileEncoded + "\n");
                    break;
                case 4:
                    System.out.println("\nEnter the name of the image file you want to decode:");
                    System.out.print(">> ");
                    String imageWithEncodedFile = sc.next();
                    String decodedFilename = s.extractFile(imageWithEncodedFile);
                    if(decodedFilename.equals("Fail")) System.out.println("Failed to decode a file from the specified image file!");
                    else System.out.println("\nThe decoded filename is: " + decodedFilename + "\n");
                    break;
	    		case 5:
	    			stop = true;
	    			System.exit(0);
	    			break;
	    		default:
	    			System.out.println("\nPlease choose a number from 1 to 5 as specified in the menu!");
	    			break;
	    	}
    		
    	}
    	sc.close();
    } 
}
