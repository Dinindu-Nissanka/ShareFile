/**
 * Created by Dinindu on 1/15/2017.
 */
public class Neighbour{
	private String ip;
	private int port;	

	public Neighbour(String ip, int port){
		this.ip = ip;
		this.port = port;
	}	

	public String getIp(){
		return this.ip;
	}

	public int getPort(){
		return this.port;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
	        return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    Neighbour n = (Neighbour) obj;
		if((n.getIp().equals(this.ip)) && (n.getPort()==this.getPort()))
			return true;
		return false;
	}
}
