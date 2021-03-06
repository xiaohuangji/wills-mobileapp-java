package com.wills.clientproxy;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wills.clientproxy.zookeeper.HessianClusterZookeeperFacade;
 

/**
 * @author huangsiping
 *
 */
public class HessianClusterNodeChangeHandler implements NodeChangeHandler {
   
	private static final Logger logger = LoggerFactory
			.getLogger(HessianClusterNodeChangeHandler.class);
	
	private ClusterNodeManager clusterNodeManager;
	
	public HessianClusterNodeChangeHandler(
			ClusterNodeManager clusterNodeManager) {
		super(); 
		this.clusterNodeManager = clusterNodeManager;
	}

	@Override
	public void process(WatchedEvent event) {
		logger.info("----node children "+event.toString()); 
		if(event.getType().equals(Watcher.Event.EventType.NodeChildrenChanged)){
			String path = event.getPath(); 
			List<String> rawNodes = HessianClusterZookeeperFacade.getInstance().lookupClusterNodes(path,this);
			
			//只观察新建node
			for(final String node : rawNodes){ 
				//对于新到的服务器节点，设置监视器观察其enable disable情况
				try {
					clusterNodeManager.getManagedNodeNames();
					if (!clusterNodeManager.getManagedNodeNames()
							.contains(node)) {
						// 不等zkServer发送enable动作，直接查询到有新节点且是enable状态的就加入集群列表，彻底解决掉线重连问题
						String dt = HessianClusterZookeeperFacade.getInstance()
								.getNodeData(path + "/" + node);
						if (Constants.ENABLE_STATUS.equals(dt)) {
							clusterNodeManager.addNode(node);
						}
						
					}
					HessianClusterZookeeperFacade.getInstance().bindClusterNodeChangeHandler(path + "/" + node,clusterNodeManager.getNodeDataChangeHandler());

					
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		 
		}
		
		
	}

}
