package com.wills.clientproxy.zookeeper;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tg.util.CONFIGUtil;
 
/**
 * @author huangsiping
 *
 */
public class HessianClusterZookeeperFacade  {
	
	protected static Logger logger = LoggerFactory.getLogger(HessianClusterZookeeperFacade.class.getName());
 
	private final String DEFAULT_CONNECT_STRING = CONFIGUtil.getInstance().getConfig("zk_host");//"renren.soundsns.registry:2181";
	
	private final String DEFALUT_SERVER_NAME = "ZKServer" ;
			
	private static HessianClusterZookeeperFacade instance = new HessianClusterZookeeperFacade();
	
	private ZKOperation zkOperation = new ZKTemplate();
	
 
	private HessianClusterZookeeperFacade(){
		
	}
	
	public static HessianClusterZookeeperFacade getInstance(){
		
		return instance;
	}
	
	public void init(Watcher watcher){
		try {
			zkOperation.init(DEFAULT_CONNECT_STRING, DEFALUT_SERVER_NAME,watcher);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


	public void bindClusterNodeChangeHandler(String path,Watcher watcher) throws KeeperException, InterruptedException{
		zkOperation.bindHandler(path, watcher);
		
	}
 
	public List<String> lookupClusterNodes(String path,Watcher watcher) {  
		
		List<String> rlt = null;
		try {
			rlt = zkOperation.getChildren(path,watcher);
		}  catch(Exception e) {
			logger.error("get path children failed", e);
		} 
		
		return rlt;
		
		
	}
	
	public String getNodeData(String path,Watcher watcher)  {
		String rlt = null;
		try {
			rlt =  zkOperation.getData(path,watcher);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return rlt ;
	}
	
	public String getNodeData(String path)  {
		String rlt = null;
		try {
			rlt =  zkOperation.getData(path,null);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return rlt ;
	}
	
	public void reconnect(Watcher watcher){
		try {
			zkOperation.destroy();
			zkOperation.init(DEFAULT_CONNECT_STRING, DEFALUT_SERVER_NAME,watcher);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
