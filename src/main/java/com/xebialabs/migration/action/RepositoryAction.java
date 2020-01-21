/**
 * Copyright 2020 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryAction implements Comparable<RepositoryAction>
{
    private Integer order;
    private ActionType action;
    private ItemType type;
    private String message;
    private String taskParentType;
    private Map<String,String> properties;
    private List <ContentActionXLR> xlrContentActions;

    public RepositoryAction(Integer order, ActionType action, ItemType type)
    {
        this.order = order;
        this.action = action;
        this.type = type;
        this.message = "";
        this.properties = new HashMap<String,String>();
        this.taskParentType = "";
        this.xlrContentActions = new ArrayList <ContentActionXLR>();
    }

    public RepositoryAction(Integer order, ActionType action, ItemType type, Map<String,String> properties)
    {
        this.order = order;
        this.action = action;
        this.type = type;
        this.message = "";
        this.properties = properties;
        this.taskParentType = "";
        this.xlrContentActions = new ArrayList <ContentActionXLR>();
    }

    public RepositoryAction(Integer order, ActionType action, ItemType type,  
        Map<String,String> properties, String taskParentType, List <ContentActionXLR> xlrContentActions)
    {
        this.order = order;
        this.action = action;
        this.type = type;
        this.message = "";
        this.properties = properties;
        this.taskParentType = taskParentType;
        this.xlrContentActions = xlrContentActions;
    }

    public RepositoryAction(Integer order, ActionType action, ItemType type, String message, 
        Map<String,String> properties, String taskParentType, List <ContentActionXLR> xlrContentActions)
    {
        this.order = order;
        this.action = action;
        this.type = type;
        this.message = message;
        this.properties = properties;
        this.taskParentType = taskParentType;
        this.xlrContentActions = xlrContentActions;
    }

    public Integer getOrder()
    {
        return this.order;
    }

    public ActionType getAction()
    {
        return this.action;
    }

    public ItemType getType()
    {
        return this.type;
    }

    public String getMessage()
    {
        return this.message;
    }

    public String getTaskParentType()
    {
        return this.taskParentType;
    }

    public Map<String,String> getProperties()
    {
        if(this.properties == null){
            this.properties = new HashMap<String,String>();
        }
        return this.properties;
    }

    public void addProperty(String name, String value)
    {
        this.properties.put(name, value);
    }

    public List <ContentActionXLR> getXlrContentActions(){
        if(this.xlrContentActions == null){
            this.xlrContentActions = new ArrayList <ContentActionXLR>();
        }
        return this.xlrContentActions;
    }

    public void assertHasProperty(String propname) throws IllegalArgumentException
    {
        if ( this.properties.containsKey(propname) == false )
        {
            String msg = String.format("Action %s for type %s does not have required property '%s'", this.action.name(), this.type.name(), propname);
            throw new IllegalArgumentException(msg);
        }
    }


	@Override
	public int compareTo(RepositoryAction o) {
		return this.order.compareTo(o.getOrder());
    }
    
    @Override
	public String toString() {
        String xlrContentActionStr = "";
        if(this.xlrContentActions != null)
        {
            for (ContentActionXLR conAction : this.xlrContentActions){
                xlrContentActionStr = xlrContentActionStr + conAction;
            }
        }
        return ("order = "+this.order+", action = "+this.action.name()+", type = "
        +this.type.name()+", message = "+this.message+", taskParentType = "+this.taskParentType+", properties = "+this.properties
        +", xlrContentActions = "+xlrContentActionStr);
	}
}