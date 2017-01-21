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

	/**
	 * Get the Neighbour IP address
     *
	 * @return
	 */
	public String getIp(){
		return this.ip;
	}

    /**
     * Get the neighbour port number
     *
     * @return
     */
    public int getPort(){
		return this.port;
	}

    /**
     * Edited equals method to check the equality of the nodes
     *
     * @param obj
     * @return
     */
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
