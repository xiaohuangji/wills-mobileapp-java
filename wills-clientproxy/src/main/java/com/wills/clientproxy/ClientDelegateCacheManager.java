package com.wills.clientproxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

import com.wills.clientproxy.zookeeper.HessianClusterZookeeperFacade;
import com.wills.proxy.schedule.RandomIterator;
import com.wills.proxy.schedule.RoundRobinIterator;

/**
 * @author huangsiping
 * 
 */
public class ClientDelegateCacheManager implements InstanceCacheManager,ClusterNodeManager {

	private static final Logger logger = LoggerFactory
			.getLogger(ClientDelegateCacheManager.class);

	private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private HashMap<String, HashMap<String, Object>> nodeMap = new HashMap<String, HashMap<String, Object>>();
 
	
	
	private IterationStrategy nodeIteratorStrategy = IterationStrategy.RoundRobin;
	
	private RoundRobinIterator<HessianClusterNode> roundRobinIterator;
	
	private RandomIterator<HessianClusterNode> randomIterator;
	
	private NodeChangeHandler nodeChildrenChangeHandler = new HessianClusterNodeChangeHandler(this);
	 
	
	private final ClusterServiceRegistry registry;
	
	public ClientDelegateCacheManager(ClusterServiceRegistry registry){
		this.registry = registry;
	}
	

	public Object getServiceInstance(InstanceKey key) {
		readWriteLock.readLock().lock();
		Object instance = null;
		try {
			HashMap<String, Object> node = (HashMap<String, Object>) nodeMap
					.get(key.getNodekey());
			if (node != null) {
				instance = node.get(key.getServicekey());
			}
		} catch (Exception ex) {

		} finally {
			readWriteLock.readLock().unlock();
		}
		
		return instance;

	}

	@Override
	public Set<String> getManagedNodeNames(){
		readWriteLock.readLock().lock();
		Set<String> rtl =null;
		try{
			rtl = nodeMap.keySet();
		}catch(Exception ex){
			
		}finally{
			readWriteLock.readLock().unlock();
		}
		return rtl;
	}
	
	@Override
	public void putServiceInstance(InstanceKey key, Object instance) {
		

		try {
			readWriteLock.writeLock().lock();
			
			HashMap<String, Object> node = nodeMap.get(key.getNodekey());
			if (node == null) {
				node = new HashMap<String, Object>();
			}
			nodeMap.put(key.getNodekey(), node);
			node.put(key.getServicekey(), instance);
			 

		} catch (Exception ex) {
			logger.error(ex.getMessage());
			
		} finally {

			readWriteLock.writeLock().unlock();
		}

	}

	public void removeNode(String nodeKey) {
		
		try {
			readWriteLock.writeLock().lock();
			nodeMap.remove(nodeKey);
			refreshNodeIterator();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}

	}
	
	private void refreshNodeIterator(){
		List<HessianClusterNode> nodes = new ArrayList<HessianClusterNode>();
		for(String nodestr : nodeMap.keySet()){
			nodes.add(new HessianClusterNode(nodestr));
		} 
		 
		if(this.nodeIteratorStrategy==IterationStrategy.RoundRobin){
			this.roundRobinIterator = new RoundRobinIterator<HessianClusterNode>(nodes);
		}
		if(this.nodeIteratorStrategy == IterationStrategy.Random){
			this.randomIterator = new RandomIterator<HessianClusterNode>(nodes);
		}
	}

	@Override
	public void refreshClusterNodes(List<String> newNodes) {
		logger.info("refreshing client node cache with enabled nodes ");
		readWriteLock.writeLock().lock();		
		try { 
			Iterator<String> it = nodeMap.keySet().iterator();
			while(it.hasNext()){
				String node = it.next();
				if(!newNodes.contains(node)){
					nodeMap.remove(node);
				}
				
			}
			for(String incomingNode : newNodes){
				if(!nodeMap.containsKey(incomingNode)){
					nodeMap.put(incomingNode,new HashMap<String,Object>());
				}
			}
			
			refreshNodeIterator();
			
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();
		}	
	}

	@Override
	public void addNode(String node) {
		readWriteLock.writeLock().lock();
		try{
			nodeMap.put(node, new HashMap<String,Object>());
			refreshNodeIterator();
		}catch(Exception e){
			 logger.error(e.getMessage());
		}finally{
			readWriteLock.writeLock().unlock();
		}
		
		 
	}
	
	

	@Override
	public HessianClusterNode getAvailableNodeByStraitegy() {
		Iterator<HessianClusterNode> it = null;    
		if(this.nodeIteratorStrategy==IterationStrategy.RoundRobin){
			if(roundRobinIterator==null){
				
			} 
			it = roundRobinIterator.iterator();
			 
		}
		if(this.nodeIteratorStrategy==IterationStrategy.Random){
			if(randomIterator ==null){
				
			}
			it = randomIterator.iterator();
		}
		HessianClusterNode node = it.next(); 
		if(node!=null)
			logger.debug(node.getNode());
		
		return node;
	}


	@Override
	public NodeChangeHandler getGroupChildrenChangeHandler() {
		return this.nodeChildrenChangeHandler;
	}


	public void init() throws Exception {
		HessianClusterZookeeperFacade.getInstance().init(this.getClusterStateHandler());
		String path = registry.getClusterServiceIdId() + "/"+Constants.HOST_PREFIX;
		List<String> rawNodes = HessianClusterZookeeperFacade.getInstance()
				.lookupClusterNodes(path,nodeChildrenChangeHandler);
		if( rawNodes==null){
			logger.warn("no server enable"); 
		}
		List<String> enabledNodes = new ArrayList<String>();
		for (final String node : rawNodes) {
			 
			String data = HessianClusterZookeeperFacade.getInstance()
					.getNodeData(path + "/"+node);
			//对于每个查询到的服务器节点，设置监视器观察其enable disable情况
			HessianClusterZookeeperFacade.getInstance().bindClusterNodeChangeHandler(path + "/"+node, getNodeDataChangeHandler());
			if (Constants.ENABLE_STATUS.equals(data)) {
				enabledNodes.add(node);
			}
		}
		if(  enabledNodes.isEmpty()&&rawNodes!=null){
			//这种情况没必要抛出异常
			logger.warn("found server node(s) but no one enable");
			
		}
		this.refreshClusterNodes(enabledNodes);

	}

	@Override
	public void reconnect() throws Exception { 
		String path = registry.getClusterServiceIdId() + "/"+Constants.HOST_PREFIX;
		logger.info("start reconstructing client nodes cache...");
		List<String> rawNodes = HessianClusterZookeeperFacade.getInstance()
				.lookupClusterNodes(path,nodeChildrenChangeHandler);
		if( rawNodes==null){
			logger.info("read zk node path but no node found ,likely \" getPath failed \" happend"); 
			logger.info("existing node cache not refreshed!"); 
			return;
		}
		List<String> enabledNodes = new ArrayList<String>();
		for (final String node : rawNodes) {
			 
			String data = HessianClusterZookeeperFacade.getInstance()
					.getNodeData(path + "/"+node);
			//对于每个查询到的服务器节点，设置监视器观察其enable disable情况
			HessianClusterZookeeperFacade.getInstance().bindClusterNodeChangeHandler(path + "/"+node, getNodeDataChangeHandler());
			if (Constants.ENABLE_STATUS.equals(data)) {
				enabledNodes.add(node);
			}
		}
		if(  enabledNodes.isEmpty()&&rawNodes!=null){
			//这种情况没必要抛出异常
			logger.warn("found server node(s) but no one enable");
		}
		refreshClusterNodes(enabledNodes);

	}
	
	

	@Override
	public HessianClusterStateHandler getClusterStateHandler() {
		 return new HessianClusterStateHandler(this);
	}


	@Override
	public NodeChangeHandler getNodeDataChangeHandler() {
		return new HessianClusterNodeDataChangeHandler(this);
	}


	@Override
	public ClusterServiceRegistry getClusterServiceRegistry() {
		return this.registry;
	}

 
 

}
