package com.wills.clientproxy;


import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wills.clientproxy.zookeeper.HessianClusterZookeeperFacade;

/**
 * @author siping.huang
 * 
 */
public class ClusterNodeBuilder {

	private static final Logger logger = LoggerFactory
			.getLogger(ClusterNodeBuilder.class);
	
	private static HashMap<ClusterServiceRegistry,ClusterNodeManager> cached =  new HashMap<ClusterServiceRegistry,ClusterNodeManager>();
	
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	
	public static ClusterNodeManager  buildClusterNodeManager(ClusterServiceRegistry registry) {
		lock.readLock().lock();
		ClusterNodeManager clusterNodeManager = cached.get(registry);
		
		lock.readLock().unlock();
		if(clusterNodeManager!=null){
			return clusterNodeManager;
		}
		
		if(clusterNodeManager==null){
			
			lock.writeLock().lock();
			clusterNodeManager = new ClientDelegateCacheManager(registry);
		
			try {
				//绑定service节点
				HessianClusterZookeeperFacade.getInstance().init(clusterNodeManager.getClusterStateHandler());
				HessianClusterZookeeperFacade.getInstance().bindClusterNodeChangeHandler(registry.getClusterServiceIdId(), clusterNodeManager.getGroupChildrenChangeHandler());
				clusterNodeManager.init();
				cached.put(registry, clusterNodeManager);
			} catch (KeeperException e) {
				logger.error(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				lock.writeLock().unlock();
			}
		
			
		}	
		return clusterNodeManager;
	}

}
