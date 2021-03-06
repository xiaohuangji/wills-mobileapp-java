package com.tg.client;

import java.util.List;

import com.tg.model.GuideEvent;
import com.tg.service.GuideEventService;
import com.wills.clientproxy.ClusterServiceRegistry;
import com.wills.clientproxy.HessianDelegateFactory;

public class GuideEventServiceDelegate implements GuideEventService{

	private GuideEventService guideEventServiceDelegate;
	
	public GuideEventServiceDelegate(){
		guideEventServiceDelegate = HessianDelegateFactory.getInstance().retrieveService(new ClusterServiceRegistry(Constants.ClUSTER_PREFIX), GuideEventService.class);

	}
	@Override
	public int accept(long eventId, int guideId, int userId) {
		// TODO Auto-generated method stub
		return guideEventServiceDelegate.accept(eventId, guideId, userId);
	}

	@Override
	public int refuse(long eventId, int guideId, int userId) {
		// TODO Auto-generated method stub
		return guideEventServiceDelegate.refuse(eventId, guideId, userId);
	}

	@Override
	public List<GuideEvent> getHistoricalGuideEvents(int guideId, int start,
			int count,boolean filter) {
		// TODO Auto-generated method stub
		return guideEventServiceDelegate.getHistoricalGuideEvents(guideId, start, count,filter);
	}

	@Override
	public GuideEvent getOneGuideEvent(int guideId, long eventId) {
		// TODO Auto-generated method stub
		return guideEventServiceDelegate.getOneGuideEvent(guideId, eventId);
	}

	public static void main(String[] args) {
		GuideEventServiceDelegate d=new GuideEventServiceDelegate();
		//System.out.println(d.refuse(3, 10000005, 10000006));
		List<GuideEvent> ggEvents=d.getHistoricalGuideEvents(10000009, 0, 100, false);
		System.out.println(ggEvents.get(0).getSaContent());
	}
}
