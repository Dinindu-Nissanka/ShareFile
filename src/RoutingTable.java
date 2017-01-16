/**
 * Created by Dinindu on 1/15/2017.
 */
import java.util.ArrayList;
import java.util.List;


public class RoutingTable {

	private List<Neighbour> neighbourNodes = new ArrayList<Neighbour>();
	
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
	
	public void removeNeighbour(String ip, int port)
	{
		Neighbour tempNeighbour = new Neighbour(ip, port);
		for (int i=0;i<neighbourNodes.size();i++)
		{
			if(neighbourNodes.get(i).equals(tempNeighbour))
			{
				neighbourNodes.remove(i);
				i--;
			}
		}
	}
	
	public ArrayList<Neighbour> getNeighbours()
	{
		return (ArrayList<Neighbour>) neighbourNodes;
	}
	
	public void printRoutingTable()
	{
		for(int i=0;i<neighbourNodes.size();i++)
		{
			System.out.println(neighbourNodes.get(i).getIp() + " - "+neighbourNodes.get(i).getPort());
		}
	}
}
