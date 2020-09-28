package me.w1992wishes.common.thrift.pool;

import com.google.common.collect.Lists;

import java.util.List;


/**
 * Thrift server 包装
 */
public final class ThriftServer {
	
	private static final String SERVER_SEPARATOR = ":";
	private static final String LIST_SEPARATOR = ",";
	
	private final String host;
	private final int port;
	private final int weight;
	
	public ThriftServer(String serverConfig) {
		String[] split = serverConfig.split(SERVER_SEPARATOR);
		this.host = split[0];
		this.port = Integer.parseInt(split[1]);
		if (split.length > 2) {
			this.weight = Integer.parseInt(split[2]);
		} else {
			this.weight = 1;
		}
	}
	
	public static List<ThriftServer> parse(String servers) {
		return parse(servers.split(LIST_SEPARATOR));
	}
	
	public static List<ThriftServer> parse(String[] servers) {
		List<ThriftServer> serverList = Lists.newArrayList();
		for (String item : servers) {
			serverList.add(convert(item));
		}
		return serverList;
	}
	
	public static ThriftServer convert(String config){
		return new ThriftServer(config);
	}
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getWeight() {
		return weight;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ThriftServer other = (ThriftServer) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        return port == other.port;
    }

}
