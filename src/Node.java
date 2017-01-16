/**
 * Created by Dinindu on 1/15/2017.
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.*;


public class Node implements Runnable {
	
	private int node_port = 50000;
	private String node_username = "dinindu";
	private String node_ip = "127.0.0.1";
	private RoutingTable node_routing_table = new RoutingTable();
	private Random ran = new Random();
	private int file_number = 0;
	private int TTL_hops = 2;
	private String[] searchQueryBuffer = null;
	
	private DatagramSocket socket = null;

	public Node() throws SocketException {
		// TODO Auto-generated constructor stub
		this.node_port = ran.nextInt(10000) + 50000;
		socket = new DatagramSocket(this.node_port);
		System.out.println("Node started. Host - "+this.node_ip+", Port - "+this.node_port);
		
		file_number = (int)(Math.random()*19) + 1;
		System.out.println("Related text file for this node is "+ file_number+".txt");

		this.searchQueryBuffer = new String[10];
		this.handleQuerySearch(socket);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			registerServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true)
		{
			byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            try {
				socket.receive(incoming);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            byte[] data = incoming.getData();
            String received_string = new String(data, 0, incoming.getLength());

            //echo the details of incoming data - client ip : client port - client message
            System.out.println(incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - " + received_string);

            StringTokenizer st = new StringTokenizer(received_string, " ");
            String length = st.nextToken();
            String command = st.nextToken();
            String response = "";

            switch(command)
            {
            case "JOIN":
            	try {
					response = handleJOINRequest(socket,st);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	break;
            	
            case "LEAVE":
            	break;
            	
            case "SER":
				try {
                    response = handleSERRequest(incoming, socket , st);
                } catch (IOException e) {
                    e.printStackTrace();
                }
				break;
            	
            default:
            	response = "ERROR";
    			response = String.format("%04d", response.length() + 5) + " " + response;
            }
            
            
		}
	}

	public String handleSERRequest(DatagramPacket received, DatagramSocket socket, StringTokenizer st) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
        String ip_address = st.nextToken();
        String pPort = st.nextToken();
		int port = Integer.parseInt(pPort);
		InetAddress address = InetAddress.getByName(ip_address);
		String response = "SEROK";

		String file_name = st.nextToken();
		String hops = st.nextToken();
		String ttl = st.nextToken();

		int hop_count = Integer.parseInt(hops);
        int ttl_hops = Integer.parseInt(ttl);
        String searchQueryString = ip_address+" "+pPort+" "+file_name;//+" "+hops;

        if(isQueryInBuffer(searchQueryString))
        {
            boolean file_exist = checkFileExistency(socket,address,port,file_name,hop_count,ttl_hops);
            if(!file_exist)
                forwardSERQuery(received,socket,address,port,file_name,hop_count,ttl_hops);
        }
        else
		{
		    addQueryToBuffer(searchQueryString);
            boolean file_exist = checkFileExistency(socket,address,port,file_name,hop_count,ttl_hops);
            if(!file_exist)
                forwardSERQuery(received,socket,address,port,file_name,hop_count,ttl_hops);
        }
        return null;
    }

    private void addQueryToBuffer(String searchQueryString)
    {
        int i=0;
        for(i=0;i<searchQueryBuffer.length;i++)
        {
            if(searchQueryBuffer[i] == null)
                break;
        }
        if(i == searchQueryBuffer.length)
        {
            searchQueryBuffer[0] = searchQueryString;
        }
        else
        {
            searchQueryBuffer[i] = searchQueryString;
        }
    }

    private String forwardSERQuery(DatagramPacket received, DatagramSocket socket, InetAddress address, int port,
                                 String file_name, int hop_count, int ttl_hops) throws UnknownHostException {
        if(ttl_hops<=0)
            return null;

        hop_count++;
        ttl_hops--;
        String response = "SER "+address.getHostAddress()+" "+port+" "+file_name+" "+hop_count+" "+ttl_hops;
        response = String.format("%04d", response.length() + 5) + " " + response;

        for (int i=0;i<this.node_routing_table.getNeighbours().size();i++) {
            int neighbour_port = this.node_routing_table.getNeighbours().get(i).getPort();
            InetAddress neighbour_address = InetAddress.getByName(this.node_routing_table.getNeighbours().get(i).getIp());
            DatagramPacket searchForward = new DatagramPacket(response.getBytes(), response.getBytes().length, neighbour_address, neighbour_port);
            try {
                socket.send(searchForward);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return response;
    }

    private boolean checkFileExistency(DatagramSocket socket, InetAddress address, int port, String file_name,
                                       int hop_count, int ttl_hops) throws FileNotFoundException, IOException{

        int no_of_results = 0;
        String result_files = "";
        String response = "SEROK";
        String line;

        try (
                InputStream fis = new FileInputStream("src/contents/"+file_number+".txt");
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while ((line = br.readLine()) != null) {
                // Deal with the line
                //String lineLowercase = line.toLowerCase();
                //String file_nameLowercase = file_name.toLowerCase();
                if(Pattern.compile(Pattern.quote(file_name), Pattern.CASE_INSENSITIVE).matcher(line).find())
                {
                    result_files += line+" ";
                    no_of_results++;
                }
            }

            if(no_of_results==0)
            {
                return false;
            }
            else
            {
                hop_count++;
                response = response+" "+no_of_results+" "+this.node_ip+" "+this.node_port+" "+hop_count+" "+result_files;
                response = String.format("%04d", response.length() + 5) + " " + response;
                DatagramPacket searchReply = new DatagramPacket(response.getBytes() , response.getBytes().length , address , port);
                try {
                    socket.send(searchReply);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
	    return true;
    }

	/*
    private int isHopCountSmaller(String ipaddress, String pPort, String file_name, String hops)
    {
        for(int i=0;i<searchQueryBuffer.length;i++)
        {
            StringTokenizer stQuery = new StringTokenizer(searchQueryBuffer[i], " ");
            String queryIP = stQuery.nextToken();
            String queryPort = stQuery.nextToken();
            String queryFileName = stQuery.nextToken();
            String queryHops = stQuery.nextToken();

            if((queryIP.equals(ipaddress))&&(queryPort.equals(pPort))&&(queryFileName.equals(file_name)))
            {
            	if(Integer.parseInt(queryHops) < Integer.parseInt(hops))
            		return 1;
            	else
            		return 0;
			}
        }
        return 2;
    }
    */

    private boolean isQueryInBuffer(String searchQueryString)
    {
	    for(int i=0;i<searchQueryBuffer.length;i++)
	    {
	        if((searchQueryBuffer[i]!=null)&&(searchQueryBuffer[i].equals(searchQueryString)))
	            return true;
        }
        return false;
    }

	public String handleJOINRequest(DatagramSocket socket, StringTokenizer st) throws UnknownHostException
	{
        String ip_address = st.nextToken();
        String pPort = st.nextToken();

		int port = Integer.parseInt(pPort);
		InetAddress address = InetAddress.getByName(ip_address);
		String response = "";
		
		try{
			this.node_routing_table.addNeighbour(ip_address, port);
			System.out.println("Successfully connected with "+ip_address+" - "+port);
			response = "JOINOK 0";
			response = String.format("%04d", response.length() + 5) + " " + response;
			
		}catch(Exception e)
		{
			response = "JOINOK 9999";
			response = String.format("%04d", response.length() + 5) + " " + response;
		}
		
		DatagramPacket joinReply = new DatagramPacket(response.getBytes() , response.getBytes().length , address , port);
        try {
			socket.send(joinReply);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return response;
	}
	
	public void registerServer() throws IOException
	{
		int response_type = 9999;
		
		while((response_type==9999)||(response_type==9998)||(response_type==9997)||(response_type==9996))
		{
			String bsRequestString = "REG " + this.node_ip +" "+this.node_port+" "+ this.node_username;
			InetAddress address = InetAddress.getByName(this.node_ip);
			bsRequestString = String.format("%04d", bsRequestString.length() + 5) + " " + bsRequestString;			
			DatagramPacket bsRequest = new DatagramPacket(bsRequestString.getBytes() , bsRequestString.getBytes().length,address,55555);
	        socket.send(bsRequest);
	        
	        
	        byte[] buffer = new byte[65536];
	        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
	        socket.receive(incoming);

	        byte[] data = incoming.getData();
	        String response = new String(data, 0, incoming.getLength());

	        StringTokenizer st = new StringTokenizer(response, " ");

	        String length = st.nextToken();
	        String command = st.nextToken();
	        String no_nodes = st.nextToken();
	        int response_value = Integer.parseInt(no_nodes);
	        
	        switch(response_value){
	        	case 0: 
	        		System.out.println("Registered in the Bootstrap server as the first node");
	        		response_type = 0;
	        		break;
	        	case 1:
	        		System.out.println("Registered in the Bootstrap server as the second node");
	        		response_type = 1;
	        		this.joinNeighbours(1, st, socket);
	        		break;
	        	case 2:
	        		System.out.println("Registered in the Bootstrap server");
	        		response_type = 2;
	        		this.joinNeighbours(2, st, socket);
	        		break;
	        	case 9999:
	        		System.out.println("Error in the JOIN command");
	        		break;
	        	case 9998:
	        		System.out.println("Already a registered user. Unregister before register again");
	        		break;
	        	case 9997:
	        		System.out.println("IP and Port is registered from another user. Try different IP or Port");
	        		this.node_port = ran.nextInt(10000) + 50000;
	        		break;
	        	case 9996:
	        		System.out.println("Bootstrap server is full. Try again");
	        		break;
	        	default:
	        		break;
	        }
		}

	}
	
	public void joinNeighbours(int no_of_neighbours, StringTokenizer st, DatagramSocket socket) throws UnknownHostException
	{   
        if(no_of_neighbours == 1)
        {
        	String neighbour_1_ip = st.nextToken();
        	String neighbour_1_port = st.nextToken();
        	String response = joinNeighbour(neighbour_1_ip, neighbour_1_port, socket);
        	System.out.println(response);
        }
        else if(no_of_neighbours == 2)
        {
        	String neighbour_1_ip = st.nextToken();
        	String neighbour_1_port = st.nextToken();
        	String neighbour_2_ip = st.nextToken();
        	String neighbour_2_port = st.nextToken();
        	String response_neighbour_1 = joinNeighbour(neighbour_1_ip, neighbour_1_port, socket);
        	System.out.println(response_neighbour_1);
        	String response_neighbour_2 = joinNeighbour(neighbour_2_ip, neighbour_2_port, socket);
        	System.out.println(response_neighbour_2);
        }
	}
	
	public String joinNeighbour(String neighbour_ip,String port, DatagramSocket socket) throws UnknownHostException
	{
    	InetAddress neighbour_address = InetAddress.getByName(neighbour_ip);
    	int neighbour_port = Integer.parseInt(port);
    	
    	int join_reAttempt = 0;
    	String return_string = "Could not connect with "+neighbour_ip;
    	
    	while(join_reAttempt < 3)
    	{
    		String join_string = "JOIN " + node_ip + " " + node_port;
    		join_string = String.format("%04d", join_string.length() + 5) + " " + join_string;
    		DatagramPacket joinRequest = new DatagramPacket(join_string.getBytes() , join_string.getBytes().length,neighbour_address,neighbour_port);
            try {
				socket.send(joinRequest);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            try {
				socket.receive(incoming);
				byte[] data = incoming.getData();
	            String response = new String(data, 0, incoming.getLength());
	            StringTokenizer st = new StringTokenizer(response, " ");
	            
	            String length = st.nextToken();
	            String command = st.nextToken();
	            String value = st.nextToken();
	            int value_number = Integer.parseInt(value);
	            if(value_number==0)
	            {
	            	return_string = "Successfully connected with "+neighbour_ip+" - "+neighbour_port;
	            	node_routing_table.addNeighbour(neighbour_ip, neighbour_port);
	            	join_reAttempt = 4;
	            }
	            else
	            {
	            	join_reAttempt++;
	            }
	            
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		return return_string;
	}
	
	public void handleQuerySearch(final DatagramSocket socket)
	{
		Thread t = new Thread() {
            private Scanner scanner;

			@Override 
            public void run() {
            	while(true) {
                    try {
                        scanner = new Scanner(System.in);
                        String file_name = scanner.nextLine();
                        
                        String searchQuery = "SER "+node_ip+" "+node_port+" "+file_name+" "+0+" "+TTL_hops;
                        searchQuery = String.format("%04d", searchQuery.length() + 5) + " " + searchQuery;
                        
                        ArrayList<Neighbour> routingList = node_routing_table.getNeighbours();
                        for(int i=0;i<routingList.size();i++)
                        {
                        	String ip_address = routingList.get(i).getIp();
                        	InetAddress ip = InetAddress.getByName(ip_address);
                        	int port = routingList.get(i).getPort();
                        	DatagramPacket searchQueryPacket = new DatagramPacket(searchQuery.getBytes() , searchQuery.getBytes().length,ip,port);
                            try {
                				socket.send(searchQueryPacket);
                			} catch (IOException e) {
                				// TODO Auto-generated catch block
                				e.printStackTrace();
                			}
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
		t.start();
	}
}