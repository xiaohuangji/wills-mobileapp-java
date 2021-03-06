package com.tg.service.impl;

import org.apache.log4j.Logger;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.tg.constant.EventConstant;
import com.tg.constant.MessageConstant;
import com.tg.constant.RedisKeyConstant;
import com.tg.constant.ResultConstant;
import com.tg.dao.BroadcastEventDAO;
import com.tg.dao.GuideEventDAO;
import com.tg.dao.IdSequenceDAO;
import com.tg.dao.InviteEventDAO;
import com.tg.model.GuideEvent;
import com.tg.model.InviteEvent;
import com.tg.model.UserInfo;
import com.tg.service.InviteEventService;
import com.tg.service.MessageService;
import com.tg.service.UserService;
import com.tg.util.ListStrUtil;
import com.wills.redis.client.RedisClient;

public class InviteEventServiceImpl implements InviteEventService{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(InviteEventServiceImpl.class);

	private GuideEventDAO guideEventDAO;
	
	private InviteEventDAO inviteEventDAO;
	
	private IdSequenceDAO idSequenceDAO;
	
	private BroadcastEventDAO broadcastEventDAO;
	
	private UserService userService;
	
	private MessageService messageService;
	
	private RedisClient unreadMsgClient=new RedisClient(RedisKeyConstant.USER_UNREADMSG_COUNT);
	
	@Override
	public int invite(int userId, int guideId, String scenic, long startTime,
			long endTime) {
		// TODO Auto-generated method stub
		long eventId=idSequenceDAO.getEventId();
		//入inviteEvent库
		InviteEvent inviteEvent=new InviteEvent();
		inviteEvent.setScenic(scenic);
		inviteEvent.setCreateTime(System.currentTimeMillis());
		inviteEvent.setEndTime(endTime);
		inviteEvent.setEventId(eventId);
		inviteEvent.setEventStatus(EventConstant.IE_STATUS_PENDING);
		inviteEvent.setEventType(EventConstant.EVENT_SINGLE);
		inviteEvent.setGuideId(guideId);
		inviteEvent.setSatisfaction(EventConstant.SATIS_NONE);
		inviteEvent.setStartTime(startTime);
		inviteEvent.setUserId(userId);
		int result=inviteEventDAO.insertInvite(inviteEvent);
		if(result!=1)
			return ResultConstant.OP_FAIL;
		//入guideEvent库
		GuideEvent guideEvent=new GuideEvent();
		guideEvent.setEventId(eventId);
		guideEvent.setGuideId(guideId);
		guideEvent.setSatisfaction(EventConstant.SATIS_NONE);
		guideEvent.setStatus(EventConstant.GE_STATUS_PENDING);
		guideEvent.setUserId(userId);
		guideEvent.setCreateTime(System.currentTimeMillis());
		guideEvent.setEventType(EventConstant.EVENT_SINGLE);
		guideEvent.setStartTime(startTime);
		guideEvent.setEndTime(endTime);
		guideEvent.setScenic(scenic);	
		result=guideEventDAO.insertGuideEvent(guideEvent);
		if(result!=1)
			return ResultConstant.OP_FAIL;
		messageService.sendMessage(userId, guideId, MessageConstant.MSG_TYPE_INVITE, null);
		logger.info("invite succ,userId--guideId:"+userId+"--"+guideId);
		return ResultConstant.OP_OK;
	}

	@Override
	public int inviteAll(int userId, String scenic, long startTime,
			long endTime,String location,int gender) {
		// TODO Auto-generated method stub
		//inviteEvent库
		long eventId=idSequenceDAO.getEventId();
		//入inviteEvent库
		InviteEvent inviteEvent=new InviteEvent();
		inviteEvent.setScenic(scenic);
		inviteEvent.setCreateTime(System.currentTimeMillis());
		inviteEvent.setEndTime(endTime);
		inviteEvent.setEventId(eventId);
		inviteEvent.setEventStatus(EventConstant.IE_STATUS_PENDING);
		inviteEvent.setEventType(EventConstant.EVENT_BROADCAST);
		inviteEvent.setGuideId(0);
		inviteEvent.setSatisfaction(EventConstant.SATIS_NONE);
		inviteEvent.setStartTime(startTime);
		inviteEvent.setUserId(userId);
		int result=inviteEventDAO.insertInvite(inviteEvent);
		if(result!=1)
			return ResultConstant.OP_FAIL;
		//广播给多人
		double dist=100;
		int count=100;
		List<Integer> ids=userService.getNearByGuideWithFilter(gender, scenic, location, dist, 0, count);
		broadcastEventDAO.insertBroadCastEvent(eventId, ListStrUtil.listToStr(ids));
		//入库
		if(ids!=null){
			for(Integer id:ids){
				if(userId==id)
					continue;
				GuideEvent guideEvent=new GuideEvent();
				guideEvent.setEventId(eventId);
				guideEvent.setGuideId(id);
				guideEvent.setSatisfaction(EventConstant.SATIS_NONE);
				guideEvent.setStatus(EventConstant.GE_STATUS_PENDING);
				guideEvent.setUserId(userId);
				guideEvent.setCreateTime(System.currentTimeMillis());
				guideEvent.setEventType(EventConstant.EVENT_BROADCAST);
				guideEvent.setStartTime(startTime);
				guideEvent.setEndTime(endTime);
				guideEvent.setScenic(scenic);	
				guideEventDAO.insertGuideEvent(guideEvent);
				messageService.sendMessage(userId, id,MessageConstant.MSG_TYPE_BROADCAST, null);
			}
		}
		logger.info("inviteall succ,userId--guideId:"+userId+"--"+ids);
		return ResultConstant.OP_OK;
	}

	@Override
	public int cancle(long eventId, int userId,int guideId) {
		// TODO Auto-generated method stub
		//修改invite库
		inviteEventDAO.changeStatus(userId, eventId, EventConstant.IE_STATUS_CANCLE);
		
		InviteEvent iv=inviteEventDAO.getOneInviteEvent(userId, eventId);
		if(iv.getEventType()==EventConstant.EVENT_BROADCAST){
			List<Integer> broadcastIds=ListStrUtil.strToList(broadcastEventDAO.getBroadcastIds(eventId));
			if(broadcastIds!=null){
				for(Integer id:broadcastIds){
					guideEventDAO.changeStatus(eventId, EventConstant.GE_STATUS_CANCLED, id);
				}
			}

		}else{
			//修改guide库
			guideEventDAO.changeStatus(eventId, EventConstant.GE_STATUS_CANCLED, guideId);
		}
		logger.info("cancel invite，useId--eventId:"+userId+"--"+eventId);
		return ResultConstant.OP_OK;
	}

	@Override
	public List<InviteEvent> getHistoricalInviteEvents(int userId,int start,int count) {
		// TODO Auto-generated method stub
		List<InviteEvent> inviteEvents=inviteEventDAO.getHistoricalInviteEvents(userId, start, count);
		renderGuideInfos(inviteEvents);
		unreadMsgClient.set(String.valueOf(userId), 0);
		return inviteEvents;
	}


	@Override
	public InviteEvent getOneInviteEvent(int userId, long eventId) {
		// TODO Auto-generated method stub
		InviteEvent inviteEvent=inviteEventDAO.getOneInviteEvent(userId, eventId);
		renderGuideInfo(inviteEvent);
		return inviteEvent;
	}
	
	@Override
	public int setSatisfaction(long eventId, int satisfaction,String saContent,int userId,int guideId) {
		// TODO Auto-generated method stub
		//更新事件满意程度
		int result1=inviteEventDAO.setSatisfaction(userId, eventId, satisfaction,saContent);
		int result2=guideEventDAO.setSatisfaction(eventId, satisfaction,saContent, guideId);
		//更新导游评分信息;
		int result3=userService.updateEvaluate(guideId, satisfaction);
		//发送消息
		messageService.sendMessage(userId, guideId, MessageConstant.MSG_TYPE_EVALUATED, null);
		
		if(result1==1 && result2==1 && result3==ResultConstant.OP_OK){
			logger.info("setsatisfaction succ,userId--eventId:"+userId+"--"+eventId);
			return ResultConstant.OP_OK;
		}
		return ResultConstant.OP_FAIL;
	}

	@Autowired
	public void setGuideEventDAO(GuideEventDAO guideEventDAO) {
		this.guideEventDAO = guideEventDAO;
	}
	@Autowired
	public void setInviteEventDAO(InviteEventDAO inviteEventDAO) {
		this.inviteEventDAO = inviteEventDAO;
	}
	@Autowired
	public void setIdSequenceDAO(IdSequenceDAO idSequenceDAO) {
		this.idSequenceDAO = idSequenceDAO;
	}
	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	@Autowired
	public void setBroadcastEventDAO(BroadcastEventDAO broadcastEventDAO) {
		this.broadcastEventDAO = broadcastEventDAO;
	}

	@Autowired
	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}

	private void renderGuideInfo(InviteEvent inviteEvent){
		if(inviteEvent==null||inviteEvent.getGuideId()==0){
			return ;
		}
		UserInfo userInfo=userService.getUserInfo(inviteEvent.getGuideId());
		if(userInfo==null)
			return ;
		inviteEvent.setGuideName(userInfo.getUserName());
		inviteEvent.setGuideHeadUrl(userInfo.getHeadUrl());
		inviteEvent.setMobile(userInfo.getMobile());
	}
	
	private void renderGuideInfos(List<InviteEvent> inviteEvents){
		if(inviteEvents==null||inviteEvents.size()==0)
			return ;
		for(InviteEvent inviteEvent:inviteEvents){
			renderGuideInfo(inviteEvent);
		}
	}
	
}
