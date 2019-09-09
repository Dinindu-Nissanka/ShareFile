import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by Dinindu on 1/19/2017.
 */
public class FileShare {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        System.out.print("Please provide Bootstrap server IP Address : ");
        Scanner scanner = new Scanner(System.in);
        String bsIP = scanner.nextLine();

        Node node = new Node(bsIP);
        node.run();
    }
}
