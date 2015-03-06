/*
 * NodeInfo.java
 *
 * Created on December 13, 2006, 10:52 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.help;

import org.xml.sax.Attributes;

/**
 *
 * @author Aaron
 */
public class NodeInfo {
    
    NodeInfo(Attributes attributes) {
        if (attributes.getLength() ==1) {
            name = attributes.getValue("name");
        } else if (attributes.getLength() == 2) {
            name = attributes.getValue("name");
            url = attributes.getValue("link");
        }
    }
    
    public String toString() {
        return name;
    }
    
    String name;
    String url;
}
