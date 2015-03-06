/*
 * HelpHandler.java
 *
 * Created on December 13, 2006, 10:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.help;

import java.util.Stack;
import javax.swing.tree.DefaultMutableTreeNode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Aaron
 */
public class HelpHandler extends DefaultHandler {
    
    /** Creates a new instance of HelpHandler */
    public HelpHandler() {
        stack = new Stack<DefaultMutableTreeNode>();
    }
    
    public void startElement (String uri, String localName, String qName, 
            Attributes attributes) throws SAXException {
        if (stack.empty()) {
            root = new DefaultMutableTreeNode(new NodeInfo(attributes));
            stack.push(root);
        } else {
            DefaultMutableTreeNode parent = stack.peek();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                    new NodeInfo(attributes));
            parent.add(node);
            stack.push(node);
        }
    }
    
    public void endElement (String uri, String localName, String qName)
	throws SAXException {
	stack.pop();
    }
    
    public DefaultMutableTreeNode getNodes() {
        return root;
    }
    
    private DefaultMutableTreeNode root;
    private Stack<DefaultMutableTreeNode> stack;
}
