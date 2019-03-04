/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration.action;

import java.util.HashMap;
import java.util.Map;

public class ContentActionXLR implements Comparable<RepositoryAction>
{
    private Integer order;
    private ContentActionType action;
    private Map<String,String> properties;

    public ContentActionXLR(Integer order, ContentActionType action)
    {
        this.order = order;
        this.action = action;
        this.properties = new HashMap<String,String>();
    }

    public ContentActionXLR(Integer order, ContentActionType action, Map<String,String> properties)
    {
        this.order = order;
        this.action = action;
        this.properties = properties;
    }

    public Integer getOrder()
    {
        return this.order;
    }

    public ContentActionType getAction()
    {
        return this.action;
    }


    public Map<String,String> getProperties()
    {
        if (this.properties == null){
            this.properties = new HashMap<String,String>();
        }
        return this.properties;
    }

    public void addProperty(String name, String value)
    {
        this.properties.put(name, value);
    }


    public void assertHasProperty(String propname) throws IllegalArgumentException
    {
        if ( this.properties.containsKey(propname) == false )
        {
            String msg = String.format("ContentActionXLR %s does not have required property '%s'", this.action.name(),  propname);
            throw new IllegalArgumentException(msg);
        }
    }


	@Override
	public int compareTo(RepositoryAction o) {
		return this.order.compareTo(o.getOrder());
    }
    
    @Override
	public String toString() {
        
        return ("order = "+this.order+", action = "+this.action.name()+", properties = "+this.properties);
	}
}