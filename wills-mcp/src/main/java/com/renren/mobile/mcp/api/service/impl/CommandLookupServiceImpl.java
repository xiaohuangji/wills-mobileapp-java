/**
 * $Id: CommandLookupServiceImpl.java 2880 2013-01-21 07:23:49Z wei.cheng3 $
 * Copyright 2009-2012 Oak Pacific Interactive. All rights reserved.
 */
package com.renren.mobile.mcp.api.service.impl;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.renren.mobile.mcp.api.command.ApiCommand;
import com.renren.mobile.mcp.api.service.CommandLookupService;

/**
 * @author sunji
 * 
 */
public class CommandLookupServiceImpl implements CommandLookupService {

    private static final Log logger = LogFactory.getLog(CommandLookupServiceImpl.class);

    private Map<String, ApiCommand> apiMapByConfig;

    private Set<String> ticketUnnecessaryApiSet;

    private String[] allCommands = new String[0];

    private String[] xmlCommands = new String[0];

    @Override
    public ApiCommand lookupApiCommand(String methodValue) {
        if (this.apiMapByConfig == null) {
            return null;
        }
        return this.apiMapByConfig.get(methodValue);
    }

    @Override
    public boolean isNeedLogin(String methodValue) {
        if (this.ticketUnnecessaryApiSet == null) {
            return true;
        }
        if (this.ticketUnnecessaryApiSet.contains(methodValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getCommands() {
        return this.allCommands;
    }

    public void setApiMapByConfig(Map<String, ApiCommand> apiMapByConfig) {
        this.apiMapByConfig = apiMapByConfig;
        if (apiMapByConfig != null) {
            xmlCommands = apiMapByConfig.keySet().toArray(new String[0]);
            this.allCommands = new String[this.xmlCommands.length];
            for (int i = 0; i < xmlCommands.length; i++) {
                this.allCommands[i] = this.xmlCommands[i];
            }
            logger.info("CommandLookupServiceImpl setApiMapByConfig apiMapByConfig:"
                    + apiMapByConfig.size() + " allCommands:" + allCommands.length);
        }
    }

    public void setTicketUnnecessaryApiSet(Set<String> ticketUnnecessaryApiSet) {
        this.ticketUnnecessaryApiSet = ticketUnnecessaryApiSet;
        logger.info("CommandLookupServiceImpl setApiMapByConfig ticketUnnecessaryApiSet:"
                + ticketUnnecessaryApiSet);
    }

}
