package com.stack.gt.cse.c6140;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class ContextTree
{
	private static Node[] T; // Array of nodes to store the tree
	private static Stack<Integer> parentStack; // Stores which parent is entered
	private static Stack<Integer> childStack; // Stores which child is entered
	private static int height; // Stores height of the tree
	
	static int depth = 0; // Current depth while inserting
	static Boolean rootSplitOccurred; // The root split
	static Boolean noNodeUpdate; // Do not update nodes
	
	static Node[] T_new; // New tree
	static int[] childIdHistory; // Stores child id in new tree during rebuild
	
	static boolean DEBUG = false;
	
	public static void main(String[] args) 
	{
		// Set the branching factor for the tree
		Node.setBranchingFactor(2);
		
		// Set the threshold for the tree
		Node.setClosenessThreshold(2);
		
		parentStack = new Stack<Integer>();
		childStack  = new Stack<Integer>();
		
		// Initialize the tree to a single node
		T = new Node[1];
		T[0] = new Node();
		height = 1;
		
		// Vary the number of elements to insert to tree
		int size = 100; 
		
		// Store all entered values for debugging
		ArrayList<Integer> seq = new ArrayList<Integer>();		
		
		try
		{
			for(int i = 0; i < size; i++)
			{
				// Clear our global members
				parentStack.clear();
				childStack.clear();
				depth = -1;
				rootSplitOccurred = false;
				noNodeUpdate = false;
				
				// Generate a random value
				int value = (int)(Math.random()*100);	
				seq.add(value);
				println("Inserting: " + value);
				
				// Insert the point to the tree
				insertToTree(0, value, 1);		
				
				// Display the tree structure
				printTree(0, 0); 
			}
		}
		catch(Exception e)
		{
			// Display error information
			println("Error!");
			e.printStackTrace();
			
			println("Input values: ");
			println(seq.toString());
			println("Tree height = " + height);
			println(Arrays.toString(T));
		}
		
	}
	
	/**
	 * Inserts a new element to the tree rooted at nodeId
	 * @param nodeId
	 * @param value
	 * @param points
	 */
	public static void insertToTree(int nodeId, int value, int points)
	{
		Node node = T[nodeId];
		depth++;
		
		if(node.isLeaf())
		{
			int closestVal = node.getClosestValue(value);
			if(node.isCloseEnough(closestVal, value))
			{
				node.addValueToLoc(closestVal, value);				
			}
			else
			{
				insertToNode(nodeId, value, points);
			}
		}
		else
		{					
			int cci = node.getClosestChild(value); // index of closest child in 
												   // the node
			int cc = node.getChild(cci); // location of closest child in tree
			
			parentStack.push(nodeId); // Add current node to parent stack			
			childStack.push(cci); // Add the new child
			insertToTree(cc, value, points);

			// A rebuild occurred, this means the indices have changed and nodes
			// have already been updated. Do not update now.
			if(!noNodeUpdate)
			{
				node.updateValue(cci, T[cc].getValue());
				node.setPointsInValue(cci, T[cc].getNumPoints());
			}	
		}
	}
	
	/**
	 * Inserts a value into a given node
	 * @param nodeId
	 * @param value
	 * @param points
	 */
	public static void insertToNode(int nodeId, int value, int points)
	{
		Node node = T[nodeId];
		
		// Add the value to the node
		node.insertValue(value, points);
		
		// Split the node into two new nodes if required
		if(node.splitRequired())
		{
			splitNode(nodeId, depth);			
		}		
	}
	
	/**
	 * Splits the given node into two new nodes and updates the tree. Will
	 * recursively ascend the tree until no more nodes require splitting.
	 * @param nodeId
	 * @param nodeDepth
	 */
	public static void splitNode(int nodeId, int nodeDepth)
	{
		int n1Loc = -1;
		int n2Loc = -1;
		int parent = 0;
		int childLoc = 1;
		
		Node node = T[nodeId];
		
		// The new nodes after the split
		Node n1 = new Node();
		Node n2 = new Node();
		
		// Farthest pair of points in the node act as seeds
		int[] farthestPair = node.getFarthestPair();
		
		T[nodeId] = node;
		int[] children = node.getChildrenLocs();		
		int[] values = node.getValues();

		if(!node.isLeaf())
		{
			// Assign each child to the closest of the two seed nodes
			int numChildren = node.getNumChildren();
			for(int i = 0; i < numChildren; i++)
			{
				int d1 = T[children[i]].getDist(
								T[children[farthestPair[0]]].getValue());
				int d2 = T[children[i]].getDist(
								T[children[farthestPair[1]]].getValue());
				
				if(d1 < d2)
				{
					n1.insertChild(children[i], T[children[i]].getValue());		
					n1.setPointsInValue(n1.getNumChildren()-1, 
											T[children[i]].getNumPoints());
				}
				else
				{
					n2.insertChild(children[i], T[children[i]].getValue());
					n2.setPointsInValue(n2.getNumChildren()-1, 
											T[children[i]].getNumPoints());
				}
			}
		}
		else
		{
			// Assign each value to the closest of the two seed nodes
			int numValues = node.getNumValues();
			int[] pointsInVal = node.getPointsInVal();
			for(int i = 0; i < numValues; i++)
			{
				int d1 = (int)Math.abs(values[i] - values[farthestPair[0]]);
				int d2 = (int)Math.abs(values[i] - values[farthestPair[1]]);
				
				if(d1 < d2)
				{
					n1.insertValue(values[i], pointsInVal[i]);
				}
				else
				{
					n2.insertValue(values[i], pointsInVal[i]);
				}
			}
		}
				
		
		if(!parentStack.isEmpty()) // Split non-root
		{
			// Get the parent location and the child node id
			parent = parentStack.pop();
			childLoc = childStack.pop();
		}
		
		// If the node is node 0, the root must be split and the tree rebuilt	
		if(nodeId == 0)
		{
			rebuildTree(n1, n2);
			height++;
		}
		else
		{
			n1Loc = nodeId; // Keep the first node in the same location
			
			// Update the parameters
			T[parent].updateChild(childLoc, n1Loc, n1.getValue());
			T[parent].setPointsInValue(childLoc, n1.getNumPoints());
			T[n1Loc] = n1;

			// Determine the location of the new node in the tree
			n2Loc = n1Loc + (1 + nodesBelow(nodeDepth+1, height))*
							(T[parent].getNumChildren() - childLoc);
			
			// Insert the new node into the tree
			T[parent].insertChild(n2Loc, n2.getValue());				
			T[parent].setPointsInValue(T[parent].getNumValues()-1, 
														n2.getNumPoints());		

			// Move the children into the correct locations after the split							
			int[] childLocs = n2.getChildrenLocs();
			for(int i = 0; i < n2.getNumChildren(); i++)
			{
				int newLoc = n2Loc + 1 + i;					
				T[newLoc] = T[childLocs[i]];	
				T[childLocs[i]] = null;
				childLocs[i] = newLoc;				
			}
			
			// Update the new nodes children to point to the correct locations
			n2.setChildrenLocs(childLocs);
			T[n2Loc] = n2;
		
			
			// Fill in any holes that exist in node 1 since we removed
			// children and put them into node2
			if(!node.isLeaf())
			{
				int[] n1Vals = n1.getValues();
				int[] n1Child = n1.getChildrenLocs();
				int i = 0;
				for(i = 0; i < n1.getNumChildren(); i++)
				{
					T[n1Loc+1+i] = T[n1Child[i]];
					n1.updateChild(i, n1Loc+1+i, n1Vals[i]);					
				}
				while(i < Node.getBranchingFactor())
				{
					T[n1Loc+1+i] = null;
					i++;
				}
			}
		
			// Check if the parent requires a split
			if(T[parent].splitRequired())
			{
				splitNode(parent, nodeDepth-1);
			}
		}
	}
	
	/**
	 * Returns the given nodes depth within a subtree
	 * @param subtreeHeight
	 * @param nodeDepth
	 * @return
	 */
	public static int getDepthInSubtree(int subtreeHeight, int nodeDepth)
	{
		if(nodeDepth > subtreeHeight)
			return -1;
		
		if( subtreeHeight <= 2)
		{
			return nodeDepth;
		}
		else
		{
			int ht = (int) Math.floor(subtreeHeight/2);
			if(nodeDepth > ht)
				return getDepthInSubtree(subtreeHeight - ht, nodeDepth - ht);
			else
				return getDepthInSubtree(ht, nodeDepth);
		}
	}
	
	/**
	 * Calculates the number of nodes below a given node for a tree of a given 
	 * height
	 * @param depth
	 * @param treeHeight
	 * @return
	 */
	public static int nodesBelow(int depth, int treeHeight)
	{
		int width = 0;
		
		for(int i = depth; i < treeHeight; i++)
		{
			width += (int)Math.pow(Node.getBranchingFactor()+1,treeHeight-i);
		}
		return width;
	}
	
	/**
	 * Calculates the number of nodes between startDepth and endDepth, 
	 * inclusively.
	 * @param startDepth
	 * @param endDepth
	 * @return
	 */
	public static int nodesBetween(int startDepth, int endDepth)
	{
		return nodesAbove(endDepth+1) - nodesAbove(startDepth);	
	}
	
	/**
	 * Calculates the number of nodes above a given depth of the tree
	 * @param depth
	 * @return
	 */
	public static int nodesAbove(int depth)
	{
		int nodesAbove = 0;
		
		for(int i = 1; i < depth; i++)
		{
			nodesAbove += (int)Math.pow(Node.getBranchingFactor()+1, i-1);
		}
		return nodesAbove;
	}
	
	/**
	 * Determines the start and end location of a nodes parent tree
	 * @param start This should be set to zero
	 * @param end This should be set to the height of the tree
	 * @param nodeDepth The depth of the node within the tree
	 * @return The first element of the array is the start depth, the second 
	 * element is the end depth
	 */
	public static int[] locOfParentTree(int start, int end, int nodeDepth)
	{		
		if(end - start < 4)
			return new int[]{start, end};
		else
		{
			int mid = (end + start - 1) / 2;
			
			if(nodeDepth <= mid) 
				return locOfParentTree(start, mid, nodeDepth);
			else
				return locOfParentTree(mid+1, end, nodeDepth);
		}
	}

	/**
	 * Rebuilds the tree whenever the root node is split
	 * @param node1
	 * @param node2
	 */
	public static void rebuildTree(Node node1, Node node2)
	{
		println("\n***** Rebuild started...");
		int newSize = 0;
		rootSplitOccurred = true;
		noNodeUpdate = true;
		
		childIdHistory = new int[height+1];
		
		// Determine the size of the new tree and initialize the new tree
		newSize = nodesBelow(1, height+1) + 1;
		T_new = new Node[newSize];	
		T_new[0] = new Node();
		
		// Insert the two seed nodes to the tree
		childIdHistory[0] = 0;
		buildNew(node1, 0, 2, 0);
		childIdHistory[0] = 1;
		buildNew(node2, 1, 2, 0);
		
		// Update the original tree to be the new tree
		T = T_new; 
		
		printTree(0, 0);
		println("***** Rebuild complete\n\n");
	}

	/**
	 * Rebuilds a tree rooted at the given node
	 * @param node
	 * @param childId
	 * @param nodeDepth
	 * @param parentLoc
	 */
	public static void buildNew(
			Node node, int childId, int nodeDepth, int parentLoc)
	{		 
		// Store the children of the current node in the old tree
		int[] children = node.getChildrenLocs();		
		
		// The new tree height is one greater than the old height
		int newHeight = height + 1;
		
		// Determine the nodes depth in its subtree. Possible values are 1 or 2
		int sh = getDepthInSubtree(newHeight, nodeDepth);
		int loc;
		
		// The node is on the lower level of the subtree
		if(sh == 2)
			// Children come immediately after the parent
			loc = parentLoc + 1 + childId;
		else
		{
			// Check to see the depth of the node above this subtree
			if(getDepthInSubtree(newHeight, nodeDepth-1) == 2)
			{
				// Store where this subtree's parent tree starts and ends
				int[] treeLocs = locOfParentTree(0, newHeight, nodeDepth);
				
				// Get all of the nodes above this node
				int nodesUp = nodesAbove(nodeDepth);
				
				// Get all of the nodes to the left of this node
				int nodesBn = nodesBetween(treeLocs[0], treeLocs[1]);
				float fracBefore = 0;
				for(int i = 1; i <= nodeDepth; i++)
				{
					fracBefore += (float)childIdHistory[i-1] / 
							Math.pow(Node.getBranchingFactor(), i);
				}
				
				// Set the location of this node in the new tree
				loc = nodesUp + (int)(nodesBn * fracBefore);				
			}	
			else // sh_parent == 1
			{
				loc = parentLoc + 1 + 
						childId*(1+nodesBelow(nodeDepth, newHeight));
			}
		}
			
		// Put the current node into the new tree
		T_new[loc] = node;
		
		// Get this nodes parent
		Node parent = T_new[parentLoc];
						
		parent.insertChild(loc, node.getValue());
		parent.setPointsInValue(childId, node.getNumPoints());
		
		if(!node.isLeaf()) // non-leaf nodes have children that need to be 
			               // placed in the new tree
		{
			// Save the children of the current node and then clear them
			int[] childrenCopy = new int[children.length]; 
			System.arraycopy(children, 0, childrenCopy, 0, children.length);						
			int numChildren = node.getNumChildren();
			node.clearChildren();

			for(int i = 0; i < numChildren; i++)
			{			
				// Pass the child from the old tree into the buildNew
				childIdHistory[nodeDepth - 1] = i;
				buildNew(T[childrenCopy[i]],i,nodeDepth+1, loc);				
			}
			
		}
	}
	
	/////////////////////////////////////////////////////////////////////////
	//                SUPPORT FUNCTIONS                                   ///
	/////////////////////////////////////////////////////////////////////////
	public static void printTree(int id, int depth)
	{		
		int[] values = T[id].getValues();
		int[] points = T[id].getPointsInVal();
		
		System.out.println(genTab(depth) + id + ": " + T[id].getValue()+ " - " 
					+ Arrays.toString(values) + " " + Arrays.toString(points));
		
		for(int j = 0; j < T[id].getNumChildren(); j++)
		{
			printTree(T[id].getChild(j),depth+1);
		}
	}
	
	public static String genTab(int num)
	{
		String tab = "";
		for(int i = 0; i<num;i++) tab+="\t";
		
		return tab;
	}
	
	public static void println(String line)
	{
		if(DEBUG) 
			System.out.println(line);
	}
	public static void print(String line)
	{
		if(DEBUG) 
			System.out.print(line);
	}
	
	public static String readLine()
	{
		String s = "";
		try{
		    BufferedReader bufferRead = 
		    		new BufferedReader(new InputStreamReader(System.in));
		    s = bufferRead.readLine();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return s ;
	}
}
