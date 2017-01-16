/**
 * Created by Dinindu on 1/16/2017.
 */
public class SearchQuery {

    private String ip_address;
    private String port;
    private String file_name;
    private String hops;

    public SearchQuery(String ip_address, String port, String file_name, String hops) {
        this.ip_address = ip_address;
        this.port = port;
        this.file_name = file_name;
        this.hops = hops;
    }

    public String getIp_address() {
        return ip_address;
    }

    public String getPort() {
        return port;
    }

    public String getFile_name() {
        return file_name;
    }

    public String getHops() {
        return hops;
    }
}
