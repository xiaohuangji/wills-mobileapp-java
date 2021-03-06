package com.renren.intl.soundsns.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;

import com.tg.util.CONFIGUtil;

public class HeartBeatService implements Runnable {

	protected final Log log = LogFactory.getLog(HeartBeatService.class);

	private final String DEFAULT_CONNECT_STRING = CONFIGUtil.getInstance().getConfig("zk_host");

	private static String PROJECT_PREFIX = CONFIGUtil.getInstance().getConfig("tg_rmiserver");
	
	private final String NAMESPACE = "/hosts/";

	private final String DEFALUT_SERVER_NAME = "ZKServer";

	private static final String DISABLE_STATUS = "disabled";

	private static final String ENABLE_STATUS = "enabled";

	private static String serverStatus = DISABLE_STATUS;

	private ZKOperation zkTemplate = new ZKTemplate();

	private InetAddress addr = null;

	private String nodeName = null;

	public void init() throws IOException, KeeperException,
			InterruptedException {
		try {
			addr = NetworkUtils.getInetLocalIp();
			
		} catch (Exception ex) {
			log.info("address not in good pattern");
		}
		if(addr==null){
			nodeName = PROJECT_PREFIX + NAMESPACE + addr.getHostAddress() + "_"
					+ Constants.DEFALT_PORT;
		}else{
			nodeName = PROJECT_PREFIX + NAMESPACE + addr.getHostAddress() + "_"
					+ Constants.DEFALT_PORT;
		}
		
		zkTemplate.init(DEFAULT_CONNECT_STRING, DEFALUT_SERVER_NAME, this);

	}

	public String getServerStatus() {
		String status = null;
		try {
			status = zkTemplate.getData(nodeName);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}

	public void enableServer() throws KeeperException, InterruptedException {
		zkTemplate.changeData(nodeName, ENABLE_STATUS);
		serverStatus = ENABLE_STATUS;
		log.info("changing server "+ nodeName +" with status "+ ENABLE_STATUS);
	}

	public void disableServer() throws KeeperException, InterruptedException {
		zkTemplate.changeData(nodeName, DISABLE_STATUS);
		serverStatus = DISABLE_STATUS;
		log.info("changing server "+ nodeName +" with status "+ DISABLE_STATUS);

	}

	public Object getNodeName() {
		return nodeName;
	}

	public void destroy() {
		if (zkTemplate != null) {
			try {
				zkTemplate.destroy();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public List<Server> getAllServers() {
		List<Server> rlt = new ArrayList<Server>();
		try {
			List<String> nodeNames = zkTemplate.getChildren(PROJECT_PREFIX
					+ NAMESPACE.substring(0, NAMESPACE.length() - 1));
			for (String name : nodeNames) {
				String status = zkTemplate.getData(PROJECT_PREFIX + NAMESPACE
						+ name);
				rlt.add(new Server(PROJECT_PREFIX + NAMESPACE + name, status));
			}
		} catch (KeeperException e) {
			log.equals(e);
		} catch (InterruptedException e) {
			log.error(e);
		}
		return rlt;
	}

	@Override
	public void run() {
		try {

			if (zkTemplate.exist(nodeName)) {
				zkTemplate.delNode(nodeName);
			}
			
			//自动重连时触发客户端通知
			zkTemplate.apendTempNode(nodeName, serverStatus);
			while(zkTemplate.exist(nodeName)==false){
				Thread.sleep(1000);
			}
			if (zkTemplate.exist(nodeName)) {
				if(this.serverStatus.equals(DISABLE_STATUS)){
					this.disableServer();
				}else if(this.serverStatus.equals(ENABLE_STATUS)){
					this.enableServer();
				}
			}

		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
