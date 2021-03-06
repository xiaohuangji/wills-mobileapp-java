/**
 * $Id: ScheduleTaskSupport.java 2880 2013-01-21 07:23:49Z wei.cheng3 $
 * Copyright 2009-2012 Oak Pacific Interactive. All rights reserved.
 */
package com.renren.mobile.mcp.api.service;

import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 自定义调度
 * 
 * @author Marshal(shuai.ma@opi-corp.com) Initial Created at 2012-3-2
 */
public class ScheduleTaskSupport extends TimerTask {

    private static final Log logger = LogFactory.getLog(ScheduleTaskSupport.class);

    private int times = -1;

    private Map<Runnable, Integer> taskMap;

    /* (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        if (taskMap == null || taskMap.isEmpty()) {
            return;
        }
        times++;
        if (times < 0) {
            times = 0;
        }
        for (Runnable task : taskMap.keySet()) {
            int period = taskMap.get(task);
            if (period < 2 || times % period == 0) {
                try {
                    task.run();
                } catch (Throwable t) {
                    logger.error("run() - exception ignored", t); //$NON-NLS-1$
                }
            }
        }
    }

    public void setTaskMap(Map<Runnable, Integer> taskMap) {
        this.taskMap = taskMap;
    }

}
