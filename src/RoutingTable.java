/**
 * Created by Dinindu on 1/15/2017.
 */
import java.util.ArrayList;
import java.util.List;


/**
 * RoutingTable class
 * Each node has its routing table
 */
public class RoutingTable {

	private List<Neighbour> neighbourNodes = new ArrayList<Neighbour>();

    /**
     * Method to add a neighbour node to the routing table
     *
     * @param ip
     * @param port
     * @return
     */
    public int addNeighbour(String ip, int port)
	{
		Neighbour tempNeighbour = new Neighbour(ip, port);
		for (int i=0;i<neighbourNodes.size();i++)
		{
			if(neighbourNodes.get(i).equals(tempNeighbour))
			{
				return 0;
			}
		}
		neighbourNodes.add(new Neighbour(ip,port));
		//this.printRoutingTable();
		return 0;
	}

    /**
     * Method to remove neighbour from the routing table
     *
     * @param ip
     * @param port
     * @return
     */
    public boolean removeNeighbour(String ip, int port)
	{
		Neighbour tempNeighbour = new Neighbour(ip, port);
		for (int i=0;i<neighbourNodes.size();i++)
		{
			if(neighbourNodes.get(i).equals(tempNeighbour))
			{
				neighbourNodes.remove(i);
				System.out.println("Neighbour : "+ip+":"+port+" was removed");
				return true;
			}
		}
		return false;
	}

    /**
     * Method to return the routing table neighbour list
     *
     * @return
     */
    public ArrayList<Neighbour> getNeighbours()
	{
		return (ArrayList<Neighbour>) neighbourNodes;
	}

    /**
     * Method to print the routing table
     */
    public void printRoutingTable()
	{
		for(int i=0;i<neighbourNodes.size();i++)
		{
			System.out.println(neighbourNodes.get(i).getIp() + " - "+neighbourNodes.get(i).getPort());
		}
	}
}
