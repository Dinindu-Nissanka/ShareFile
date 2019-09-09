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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.*;


/**
 * Node class
 */
public class Node implements Runnable {
	
	private int node_port = 50000;
	private String node_username = "dinindu";
	private String node_ip = null;
	private RoutingTable node_routing_table = new RoutingTable();
	private Random ran = new Random();
	private int file_number = 0;
	private int TTL_hops = 5;
	private String[] searchQueryBuffer = null;
	private int queryBufferCount = 0;
	private String bootstrapServerIP = null;
	
	private DatagramSocket socket = null;

    /**
     * Constructor
     * Random port will be selected and new node will be created
     *
     * @throws SocketException
     * @throws UnknownHostException
     */
	public Node(String bsIP) throws SocketException, UnknownHostException {
	    this.bootstrapServerIP = bsIP;
        InetAddress nodeAddress = InetAddress.getLocalHost();
        node_ip = nodeAddress.getHostAddress();
		this.node_port = ran.nextInt(10000) + 50000;
		socket = new DatagramSocket(this.node_port);
		System.out.println("Node started. Host - "+this.node_ip+", Port - "+this.node_port);
		
		file_number = (int)(Math.random()*19) + 1;
		System.out.println("Related text file for this node is "+ file_number+".txt");

		this.searchQueryBuffer = new String[10];
		this.handleQuerySearchInput(socket);
	}

    /**
     * Run method
     * Method will handle the requested command
     */
	@Override
	public void run() {
		try {
			registerServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(true)
		{
			byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            try {
				socket.receive(incoming);
			} catch (IOException e) {
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
					e.printStackTrace();
				}
            	break;
            	
            case "LEAVE":
                try {
                    response = handleLEAVERequest(socket,st);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            	
            case "SER":
				try {
                    response = handleSERRequest(incoming, socket , received_string);
                } catch (IOException e) {
                    e.printStackTrace();
                }
				break;
            	
            case "ERROR":
                try {
                    handleERRORResponse(incoming,socket,response);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            case "SEROK":
                handleSearchOKRResponse(st);
            }
		}
	}

    private void handleSearchOKRResponse(StringTokenizer st) {
        //Timestamp time = new Timestamp(System.currentTimeMillis());
        //long timeNow = time.getTime();

	    String no_of_files = st.nextToken();
	    String ip = st.nextToken();
	    String port = st.nextToken();
	    String hops = st.nextToken();
	    String timestamp = st.nextToken();
	    //long intTimestamp = Long.parseLong(timestamp);
	    //long timeDiff = timeNow - intTimestamp;

	    //System.out.println(hops+ " - " +timeNow+ " : "+ timeDiff);
    }

    /**
     * Method to handle Error reponse
     *
     * @param incoming
     * @param socket
     * @param response
     * @throws UnknownHostException
     */
    private void handleERRORResponse(DatagramPacket incoming, DatagramSocket socket, String response) throws UnknownHostException {
        response = "ERROR";
        response = String.format("%04d", response.length() + 5) + " " + response;
        String ip = incoming.getAddress().getHostAddress();
        InetAddress ip_address = InetAddress.getByName(ip);
        DatagramPacket searchReply = new DatagramPacket(response.getBytes() , response.getBytes().length , ip_address , incoming.getPort());
        try {
            socket.send(searchReply);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to handle Leave request
     *
     * @param socket
     * @param st
     * @return
     * @throws UnknownHostException
     */
    private String handleLEAVERequest(DatagramSocket socket, StringTokenizer st) throws UnknownHostException {
        String ip_address = st.nextToken();
        String port = st.nextToken();
        String response = "LEAVEOK ";
        boolean result = node_routing_table.removeNeighbour(ip_address,Integer.parseInt(port));
        if(result)
        {
            response = response + "0";
            response = String.format("%04d", response.length() + 5) + " " + response;
            DatagramPacket searchReply = new DatagramPacket(response.getBytes() , response.getBytes().length , InetAddress.getByName(ip_address) , Integer.parseInt(port));
            try {
                socket.send(searchReply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Method to handle Search request
     *
     * @param received
     * @param socket
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String handleSERRequest(DatagramPacket received, DatagramSocket socket, String received_string) throws FileNotFoundException, IOException {

        StringTokenizer st = new StringTokenizer(received_string, "'");

        String first_section = st.nextToken();
        String file_name = st.nextToken().toLowerCase();

        StringTokenizer st2 = new StringTokenizer(first_section," ");
        String length = st2.nextToken();
        String command = st2.nextToken();
		String ip_address = st2.nextToken();
        String pPort = st2.nextToken();
		int port = Integer.parseInt(pPort);
		InetAddress address = InetAddress.getByName(ip_address);

		String response = "SEROK";

		String third_section = st.nextToken();
        StringTokenizer st3 = new StringTokenizer(third_section," ");
		String hops = st3.nextToken();
		String ttl = st3.nextToken();
		String timestamp = st3.nextToken();

		int hop_count = Integer.parseInt(hops);
        int ttl_hops = Integer.parseInt(ttl);
        String searchQueryString = ip_address+" "+pPort+" "+hops+" '"+file_name;

        if((isQueryInBuffer(ip_address,pPort,file_name,hops) == 1)||(isQueryInBuffer(ip_address,pPort,file_name,hops) == 2))
        {
            boolean file_exist = checkFileExistency(socket,address,port,file_name,hop_count,ttl_hops,timestamp);
            if(!file_exist)
                return forwardSERQuery(received,socket,address,port,file_name,hop_count,ttl_hops);

        }
        else if(isQueryInBuffer(ip_address,pPort,file_name,hops) == 0)
		{
            //System.out.println("Query is not in the buffer");
		    addQueryToBuffer(searchQueryString);
            boolean file_exist = checkFileExistency(socket,address,port,file_name,hop_count,ttl_hops,timestamp);
            if(!file_exist)
                return forwardSERQuery(received,socket,address,port,file_name,hop_count,ttl_hops);
        }
        return null;
    }

    /**
     * Method to add search query to the node query buffer
     *
     * @param searchQueryString
     */
    private void addQueryToBuffer(String searchQueryString)
    {
        int index = queryBufferCount % 10;
        searchQueryBuffer[index] = searchQueryString;
        queryBufferCount++;
    }

    /**
     * Method to forward the query to its neighbours
     *
     * @param received
     * @param socket
     * @param address
     * @param port
     * @param file_name
     * @param hop_count
     * @param ttl_hops
     * @return
     * @throws UnknownHostException
     */
    private String forwardSERQuery(DatagramPacket received, DatagramSocket socket, InetAddress address, int port,
                                 String file_name, int hop_count, int ttl_hops) throws UnknownHostException {
        if(ttl_hops<=0)
            return null;

        hop_count++;
        ttl_hops--;

        for (int i=0;i<this.node_routing_table.getNeighbours().size();i++) {
            int neighbour_port = this.node_routing_table.getNeighbours().get(i).getPort();

            String neighbour_ip = this.node_routing_table.getNeighbours().get(i).getIp();
            if(((neighbour_ip.equals(received.getAddress().getHostAddress()))&&(neighbour_port==received.getPort()))
                    ||((neighbour_ip.equals(address.getHostAddress()))&&(neighbour_port==port))) {
                //System.out.println("Addresses are equal -> "+neighbour_ip +":"+neighbour_port+
                //        " - "+received.getAddress().getHostAddress()+":"+received.getPort()+" - "+address.getHostAddress()+":"+port);
                continue;
            }
            //System.out.println("Addresses are equal -> "+neighbour_ip +":"+neighbour_port+
            //        " - "+received.getAddress().getHostAddress()+":"+received.getPort()+" - "+address.getHostAddress()+":"+port);
            InetAddress neighbour_address = InetAddress.getByName(this.node_routing_table.getNeighbours().get(i).getIp());
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String response = "SER "+address.getHostAddress()+" "+port+" '"+file_name+"' "+hop_count+" "+ttl_hops;
            response = response+ " "+timestamp.getTime();
            response = String.format("%04d", response.length() + 5) + " " + response;
            DatagramPacket searchForward = new DatagramPacket(response.getBytes(), response.getBytes().length, neighbour_address, neighbour_port);
            try {
                socket.send(searchForward);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Method to check if the file is in node's content
     *
     * @param socket
     * @param address
     * @param port
     * @param file_name
     * @param hop_count
     * @param ttl_hops
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private boolean checkFileExistency(DatagramSocket socket, InetAddress address, int port, String file_name,
                                       int hop_count, int ttl_hops, String timestamp) throws FileNotFoundException, IOException{

        int no_of_results = 0;
        String result_files = "";
        String response = "SEROK";
        String line;

        try (
                //InputStream fis = new FileInputStream("src/contents/"+file_number+".txt");
                InputStream fis = new FileInputStream("contents/"+file_number+".txt");
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while ((line = br.readLine()) != null) {
                // Deal with the line
                //String lineLowercase = line.toLowerCase();
                //String file_nameLowercase = file_name.toLowerCase();
                if(Pattern.compile("(?<=_|\\b)"+Pattern.quote(file_name)+"(?<=_|\\b)", Pattern.CASE_INSENSITIVE).matcher(line).find())
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
                response = response+" "+no_of_results+" "+this.node_ip+" "+this.node_port+" "+hop_count+" "+timestamp+" "+result_files;
                response = String.format("%04d", response.length() + 5) + " " + response;
                DatagramPacket searchReply = new DatagramPacket(response.getBytes() , response.getBytes().length , address , port);
                try {
                    socket.send(searchReply);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	    return true;
    }

    /**
     * Method to check if the query is an already received one
     *
     * @return
     */
    private int isQueryInBuffer(String ip,String port,String file_name, String hops)
    {
        String searchQueryString = ip+" "+port+" "+hops+" '"+file_name;
	    for(int i=0;i<searchQueryBuffer.length;i++)
	    {
	        if(searchQueryBuffer[i]!=null)
	        {
                if(searchQueryBuffer[i].equals(searchQueryString))
                {
                    //System.out.println("Query is in the buffer - "+searchQueryString);
                    return 1;
                }
                else
                {
                    StringTokenizer stTemp = new StringTokenizer(searchQueryBuffer[i],"'");

                    String first_section = stTemp.nextToken();
                    String buffer_file_name = stTemp.nextToken();

                    StringTokenizer stTemp2 = new StringTokenizer(first_section," ");
                    String buffer_ip = stTemp2.nextToken();
                    String buffer_port = stTemp2.nextToken();
                    String buffer_hops = stTemp2.nextToken();

                    if(ip.equals(buffer_ip)&&(port.equals(buffer_port))&&(file_name.equals(buffer_file_name)))
                    {
                        if(Integer.parseInt(hops)<Integer.parseInt(buffer_hops)) {
                            //System.out.println("Hops are smaller - "+buffer_hops+">"+hops);
                            return 2;
                        }
                        else
                            return 3;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Method to handle Join request
     *
     * @param socket
     * @param st
     * @return
     * @throws UnknownHostException
     */
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
			e.printStackTrace();
		}
        
        return response;
	}

    /**
     * Method to register with the Bootstrap server
     *
     * @throws IOException
     */
    public void registerServer() throws IOException
	{
		int response_type = 9999;
		
		while((response_type==9999)||(response_type==9998)||(response_type==9997)||(response_type==9996))
		{
			String bsRequestString = "REG " + this.node_ip +" "+this.node_port+" "+ this.node_username;
			InetAddress address = InetAddress.getByName(this.bootstrapServerIP);
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

    /**
     * Method to check the initial neighbour count of the node
     *
     * @param no_of_neighbours
     * @param st
     * @param socket
     * @throws UnknownHostException
     */
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


    /**
     * Method to send a JOIN request to a neighbour node
     *
     * @param neighbour_ip
     * @param port
     * @param socket
     * @return
     * @throws UnknownHostException
     */
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
				e.printStackTrace();
			}
    	}
		return return_string;
	}

    /**
     * Thread to get the user input for the search query
     *
     * @param socket
     */
    public void handleQuerySearchInput(final DatagramSocket socket)
	{
		Thread t = new Thread() {
            private Scanner scanner;

			@Override 
            public void run() {
            	while(true) {
                    try {
                        scanner = new Scanner(System.in);
                        String file_name = scanner.nextLine();

                        if(file_name.equals("LEAVE"))
                        {
                            String leaveQuery = "LEAVE "+node_ip+" "+node_port;
                            leaveQuery = String.format("%04d",leaveQuery.length()+5)+" "+leaveQuery;
                            String unregBootstrapServerQuery = "UNREG "+node_ip+" "+node_port+" "+node_username;
                            unregBootstrapServerQuery = String.format("%04d",unregBootstrapServerQuery.length()+5)+" "+unregBootstrapServerQuery;

                            DatagramPacket unregQueryPacket = new DatagramPacket(unregBootstrapServerQuery.getBytes() , unregBootstrapServerQuery.getBytes().length,InetAddress.getByName(bootstrapServerIP),55555);
                            try {
                                socket.send(unregQueryPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            ArrayList<Neighbour> routingList = node_routing_table.getNeighbours();
                            for(int i=0;i<routingList.size();i++)
                            {
                                String ip_address = routingList.get(i).getIp();
                                InetAddress ip = InetAddress.getByName(ip_address);
                                int port = routingList.get(i).getPort();
                                DatagramPacket searchQueryPacket = new DatagramPacket(leaveQuery.getBytes() , leaveQuery.getBytes().length,ip,port);
                                try {
                                    socket.send(searchQueryPacket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else
                        {
                            ArrayList<Neighbour> routingList = node_routing_table.getNeighbours();
                            for(int i=0;i<routingList.size();i++)
                            {
                                String ip_address = routingList.get(i).getIp();
                                InetAddress ip = InetAddress.getByName(ip_address);
                                int port = routingList.get(i).getPort();
                                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                                String searchQuery = "SER "+node_ip+" "+node_port+" '"+file_name+"' "+0+" "+TTL_hops+" "+timestamp.getTime();
                                searchQuery = String.format("%04d", searchQuery.length() + 5) + " " + searchQuery;

                                DatagramPacket searchQueryPacket = new DatagramPacket(searchQuery.getBytes() , searchQuery.getBytes().length,ip,port);
                                try {
                                    socket.send(searchQueryPacket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
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
