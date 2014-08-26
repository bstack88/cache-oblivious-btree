package com.stack.gt.cse.c6140;

import java.util.Arrays;

/**
 * @author Brian
 * 
 */
public class Node {
	private static int BranchingFactor = 5;

	public static void setBranchingFactor(int bFactor) {
		Node.BranchingFactor = bFactor;
	}

	public static int getBranchingFactor() {
		return Node.BranchingFactor;
	}

	private static int ClosenessThreshold = 10;

	public static void setClosenessThreshold(int closenessThreshold) {
		Node.ClosenessThreshold = closenessThreshold;
	}

	public static int getClosenessThreshold() {
		return Node.ClosenessThreshold;
	}

	// Note that the node does not point to its parent. This would require for
	// every node to be updated/touched whenever the tree is restructured which
	// is not feasible. Instead, the insert/update routines will manage parents
	private int[] childrenLocs; // array of children indexes
	private int[] values;
	private int valsTot; // stores the sum of values
	private int pointsTot; // stores the sum of data points
	private int[] pointsInVal;
	private int numChildren;
	private int numValues;
	private Boolean isLeaf;

	private int[] farthestPair;
	private int farthestDist;

	public Node() {
		init();
	}

	private void init() {
		childrenLocs = new int[Node.BranchingFactor + 1];
		values = new int[Node.BranchingFactor + 1];
		pointsInVal = new int[Node.BranchingFactor + 1];

		isLeaf = true;
		numChildren = 0;
		numValues = 0;

		farthestPair = new int[] { 0, 0 };
		farthestDist = 0;
		valsTot = 0;
		pointsTot = 0;
	}

	// Adds a new child id to the children array and inserts the child's value
	// into the values array
	/**
	 * Inserts the location of a child in the tree to this nodes children
	 * 
	 * @param childLoc
	 *            Location of the child in the complete tree
	 * @param value
	 *            Value of the child
	 * @return True if the child was inserted, False if the node requires a
	 *         split
	 */
	public Boolean insertChild(int childLoc, int value) {
		if (splitRequired())
			return false;

		if (isLeaf == true) {
			// reset the counters since we are no longer a leaf
			numValues = 0;
			numChildren = 0;
			valsTot = 0;
			pointsTot = 0;
			pointsInVal = new int[Node.BranchingFactor + 1];
		}

		childrenLocs[numChildren] = childLoc;
		values[numValues] = value;

		pointsTot++;
		pointsInVal[numChildren]++;
		valsTot += value;
		isLeaf = false;

		updateFurthestPair();

		numChildren++;
		numValues++;

		return true;
	}

	public void clearChildren() {
		init();
	}

	public void insertValue(int value, int points) {
		
		values[numValues] = value;
		pointsInVal[numValues] = points;

		// Keep track of total number of values
		valsTot += value*points;
		pointsTot += points;

		updateFurthestPair();

		numValues++;
	}

	public void insertValue(int value) {
		insertValue(value, 1);
	}

	public void updateFurthestPair() {
		// Check all previous children to see if this new point is part of
		// the farthest pair
		for (int i = 0; i < numValues; i++) {
			int dist = Math.abs(values[i] - values[numValues]);
			if (dist > farthestDist) {
				farthestDist = dist;
				farthestPair[0] = i;
				farthestPair[1] = numValues;
			}
		}
	}

	/**
	 * @param locInValues
	 *            The index of the element to be updated in the node
	 * @param value
	 *            The value to set the element's value to after the update
	 */
	public void updateValue(int locInValues, int value) {
		valsTot -= values[locInValues]*pointsInVal[locInValues];
		valsTot += value;
		values[locInValues] = value;
		
		pointsTot -= pointsInVal[locInValues];
		pointsInVal[locInValues] = 1;
		pointsTot++;		
	}

	/**
	 * Adds a new data point to the current value at locInValues. This function
	 * is used to allow values to be absorbed by clusters that are close enough.
	 * 
	 * @param locInValues
	 * @param value
	 */
	public void addValueToLoc(int locInValues, int value) {
		// Occurs when the first value is entered into the node
		if (pointsInVal[locInValues] == 0)
			numValues++;

		int newVal = (int)Math.round((double)(values[locInValues] * 
							pointsInVal[locInValues] + value)
				/ (double)(pointsInVal[locInValues] + 1));
		
		
		valsTot -= values[locInValues]*pointsInVal[locInValues];
		values[locInValues] = newVal;

		pointsInVal[locInValues]++;
		valsTot += newVal*pointsInVal[locInValues];
		pointsTot++;
	}

	/**
	 * Checks if a new data point is close enough to the supplied cluster to be
	 * absorbed by the cluster.
	 * 
	 * @param locInValues
	 * @param value
	 * @return
	 */
	public Boolean isCloseEnough(int locInValues, int value) {
		if (getDistance(locInValues, value) < Node.ClosenessThreshold)
			return true;
		else
			return false;
	}

	private int getDistance(int locInValues, int value) {
		return Math.abs(values[locInValues] - value);
	}

	/**
	 * @param locInChildren
	 *            The index of the element to be updated in the node
	 * @param newChildLoc
	 *            The new location of the child in the tree array
	 * @param value
	 *            The value to set the element's value to after the update
	 */
	public void updateChild(int locInChildren, int newChildLoc, int value) {
		// Update the value tracker
		valsTot -= values[locInChildren]*pointsInVal[locInChildren];
		valsTot += value;

		values[locInChildren] = value;

		childrenLocs[locInChildren] = newChildLoc;

	}

	public Boolean splitRequired() {
		return ((numChildren > Node.BranchingFactor) || 
				(numValues > Node.BranchingFactor));
	}

	public Boolean hasRoom() {
		if (isLeaf())
			return (numValues < Node.BranchingFactor);
		else
			return (numChildren < Node.BranchingFactor);
	}

	public int getNumChildren() {
		return numChildren;
	}

	public int getNumValues() {
		return numValues;
	}

	public int[] getChildrenLocs() {
		return childrenLocs;
	}

	public void setChildrenLocs(int[] childrenLocs) {
		this.childrenLocs = childrenLocs;
	}

	/**
	 * Returns the local index of the closest child to the given value
	 * 
	 * @param value
	 *            The value to find the closest child to
	 * @return The local index of the closest child to value
	 */
	public int getClosestChild(int value) {
		int mD = Integer.MAX_VALUE;
		int mL = 0;
		int dist = 0;

		for (int i = 0; i < numValues; i++) {
			dist = (int) Math.abs(value - values[i]);
			if (dist < mD) {
				mD = dist;
				mL = i;
			}
		}

		return mL;
	}

	public int getDist(int value) {
		return Math.abs(value - getValue());
	}

	/**
	 * Returns the index of the closest element to the given value
	 * 
	 * @param value
	 * @return
	 */
	public int getClosestValue(int value) {
		return getClosestChild(value);
	}

	public int[] getValues() {
		return values;
	}

	public Boolean isLeaf() {
		return isLeaf;
	}

	public int[] getFarthestPair() {
		return farthestPair;
	}

	public int getValue() {
		if (pointsTot == 0)
			return 0;
		return (int)(Math.round((double)valsTot / (double)pointsTot));
	}

	/**
	 * Returns the tree index of the nth child in this node
	 * 
	 * @param n
	 *            The nth child of this node
	 * @return The tree index of this child
	 */
	public int getChild(int n) {
		return childrenLocs[n];
	}

	public int getNextChildId() {
		return childrenLocs[numChildren - 1] + 1;
	}

	public int[] getPointsInVal() {
		return pointsInVal;
	}

	public void setPointsInValue(int cIndex, int points) {
		if (cIndex < numValues) {
			pointsTot -= pointsInVal[cIndex];
			pointsTot += points;
			valsTot += values[cIndex]*(points - 1);
			pointsInVal[cIndex] = points;
		}
	}

	public int getNumPoints() {
		return pointsTot;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append("Val = " + getValue());
		result.append("\t");
		result.append("Val tot = " + valsTot);
		result.append("\t");
		result.append("Points tot = " + pointsTot);
		result.append("\t");
		result.append("Values = " + Arrays.toString(values));
		result.append("\t");
		result.append("Points in Val = " + Arrays.toString(pointsInVal));
		result.append("\t");
		result.append("ChildrenLocs = " + Arrays.toString(childrenLocs));
		

		return result.toString();
	}

	public void print(String start) {
		System.out.println(start + ": " + toString());
	}

	public void print() {
		print("");
	}
}
