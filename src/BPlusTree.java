import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 */
public class BPlusTree<K extends Comparable<K>, T> {

	public Node<K,T> root;
	public static final int D = 2;

	/**
	 * TODO Search the value for a specific key
	 * 
	 * @param key
	 * @return value
	 */
	public T search(K key) {
		Node<K,T> currentNode = root;
		while (!(currentNode.isLeafNode)){
			currentNode = searchThroughIndex(key, (IndexNode<K, T>) currentNode);
		}
		return searchThroughLeaf(key, (LeafNode<K, T>) currentNode);
	}
	
	/**
	 * TODO Insert a key/value pair into the BPlusTree
	 * 
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {
		if (root == null){
			root = new LeafNode<K,T>(key,value);
			return;
		}
		// Special case where the root is a leaf node.
		// If it's a leaf node, then add the key, value pair into it and
		// check if it's full. If it is, split it, make a new IndexNode,
		// add the split leaves into the new IndexNode and make that IndexNode
		// the root.
		if (root.isLeafNode){
			LeafNode<K,T> tempCasted = (LeafNode<K,T>) root;
			tempCasted.insertSorted(key, value);
			if (tempCasted.getKeyArrayList().size() > 2*D){
				Entry<K, Node<K,T>> tempNewEntry = splitLeafNode(tempCasted);
				K tempKey = tempNewEntry.getKey();
				Node<K,T> tempRightChild = tempNewEntry.getValue();
				root = new IndexNode<K,T>(tempKey,root,tempRightChild);
			}
			return;
		}
		// First, insert the key/value pair into the leaf it belongs to.
		ArrayList<Node<K,T>> tempPath = searchPathOfKey(key);
		LeafNode<K,T> tempLeaf = (LeafNode<K, T>) tempPath.get(0);
		tempLeaf.insertSorted(key,value);
		// Get the lowest key in this leaf and then use that to change
		// the key in the parent index node that's directly to the left
		// of this child.
		IndexNode<K,T> tempIndexNode = (IndexNode<K,T>) tempPath.get(1);
		int theIndex = tempIndexNode.getChildrenArrayList().indexOf(tempLeaf);
		Entry<K, Node<K,T>> tempNewEntry = null;
		// If the leaf is full, split it.
		if (tempLeaf.getKeyArrayList().size() > 2*D){
			tempNewEntry = splitLeafNode(tempLeaf);
			tempIndexNode.insertSorted(tempNewEntry,theIndex);
		}
		else{
			return;
		}
		IndexNode<K,T> tempNode = (IndexNode<K, T>) tempPath.get(1);
		if (tempNode.getKeyArrayList().size() <= 2*D){
			return;
		}
		// The parent of the leaf is overflowed after this point.
		tempNewEntry = splitIndexNode(tempNode);
		// Handle the edge case where the parent of the leaf is the root,
		// by making a new IndexNode and adding the two split IndexNodes
		// to the new one and making that the root.
		if (tempPath.size() == 2){
			K newRootKey = tempNewEntry.getKey();
			root = new IndexNode<K,T>(newRootKey,root,tempNewEntry.getValue());
			return;
		}
		// Handle the general case, where an IndexNode is full. If it is,
		// split it and add the newly generated IndexNode to the parent. Then,
		// recursively apply this to the parent.
		for (int i = 2; i < tempPath.size() - 1; i++){
			tempNode = (IndexNode<K, T>) tempPath.get(i);
			Node<K,T> properChild = tempPath.get(i-1);
			theIndex = tempNode.getChildrenArrayList().indexOf(properChild);
			tempNode.insertSorted(tempNewEntry, theIndex);
			if (tempNode.getKeyArrayList().size() <= 2*D){
				return;
			}
			tempNewEntry = splitIndexNode(tempNode);
		}
		// Handle the last part of the general case, where the root is full,
		// by splitting it and making the root a new IndexNode that holds one key
		// and the old root and the split part of the old root.
		theIndex = ((IndexNode<K, T>) root).getChildrenArrayList().indexOf(tempPath.get(tempPath.size() - 2));
		((IndexNode<K, T>) root).insertSorted(tempNewEntry,theIndex);
		if (root.getKeyArrayList().size() <= 2*D){
			return;
		}
		tempNewEntry = splitIndexNode((IndexNode<K,T>) root);
		K tempRootKey = tempNewEntry.getKey();
		Node<K,T> tempRightChild =  tempNewEntry.getValue();
		root = new IndexNode<K,T>(tempRootKey,root,tempRightChild);
	}

	/**
	 * Splits an overflowed LeafNode into two Nodes, by making the
	 * original node hold the values from 0..D (excluding D) and the new (right) node
	 * hold the values from D...2D+1 (excluding 2D+1). Returns the new
	 * (right) node.
	 * 
	 * @param currentNode is the overflowed LeafNode
	 * @return the newly created LeafNode that contains
	 * the values in D+1 ... 2D from the overflowed currentNode
	 */
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> currentNode) {
		ArrayList<K> tempKeys = currentNode.getKeyArrayList();
		ArrayList<T> tempValues = currentNode.getValuesArrayList();
		ArrayList<K> tempNewKeys = new ArrayList<K>();
		ArrayList<T> tempNewValues = new ArrayList<T>();
		tempNewKeys.addAll(new ArrayList<K>(tempKeys.subList(D,2*D+1)));
		tempNewValues.addAll(new ArrayList<T>(tempValues.subList(D,2*D+1)));
		currentNode.setKeyArrayList(new ArrayList<K>(tempKeys.subList(0, D)));
		currentNode.setValuesArrayList(new ArrayList<T>(tempValues.subList(0, D)));
		LeafNode<K,T> tempLeaf = new LeafNode<K,T>(tempNewKeys,tempNewValues);
		K firstKey = tempLeaf.getKeyArrayList().get(0);
		Entry<K, Node<K,T>> tempEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(firstKey,tempLeaf);
		LeafNode<K,T> oldRight = currentNode.getRight();
		if (oldRight != null){
			oldRight.setLeft(tempLeaf);
			tempLeaf.setRight(oldRight);
		}
		currentNode.setRight(tempLeaf);
		tempLeaf.setLeft(currentNode);
		return tempEntry;
	}

	/**
	 * Splits an overflowed IndexNode into two Nodes, by making the original node
	 * hold keys from 0..D excluding D, and the children from 0..D+1 excluding D+1.
	 * The new (right) node holds keys from D..2D+1 excluding 2D+1, and 
	 * the children from D+1..2D+2, excluding 2D+2. This method also changes
	 * the key of the right IndexNode to be the smallest key in the first child's
	 * key array list.
	 * 
	 * 
	 * @param currentNode is the overflowed IndexNode
	 * @return the newly created IndexNode that contains
	 * the values in D+1 ... 2D from the overflowed currentNode
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> currentNode) {
		ArrayList<K> tempKeys = currentNode.getKeyArrayList();
		ArrayList<Node<K,T>> tempChildren = currentNode.getChildrenArrayList();
		ArrayList<K> tempNewKeys = new ArrayList<K>();
		ArrayList<Node<K,T>> tempNewChildren = new ArrayList<Node<K,T>>();
		tempNewKeys = new ArrayList<K>(tempKeys.subList(D,2*D+1));
		tempNewChildren= new ArrayList<Node<K,T>>(tempChildren.subList(D+1,2*D + 2));
		currentNode.setKeyArrayList(new ArrayList<K>(tempKeys.subList(0, D)));
		currentNode.setChildrenArrayList(new ArrayList<Node<K, T>>(tempChildren.subList(0,D+1)));
		IndexNode<K,T> tempIndex = new IndexNode<K,T>(tempNewKeys,tempNewChildren);
		K firstKey = tempIndex.getKeyArrayList().get(0);
		Entry<K, Node<K,T>> tempEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(firstKey,tempIndex);
		tempIndex.getKeyArrayList().remove(0);
		return tempEntry;
	}

	/**
	 * TODO Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {
		ArrayList<Node<K,T>> tempPath = searchPathOfKey(key);
		LeafNode<K,T> tempLeaf = (LeafNode<K, T>) tempPath.get(0);
		tempLeaf.removeKey(key);
		
		// If the root is a leaf node, then there is never any need to
		// merge or redistribute, etc.
		if (root.isLeafNode){
			return;
		}

		IndexNode<K,T> tempParent = (IndexNode<K,T>) tempPath.get(1);
		boolean isTargLeft = isTargetLeft(tempLeaf, tempParent);
		// Identify the target siblings, according to the rules given in the instructions.
		LeafNode<K,T> targetSibling;
		if(isTargLeft){
			targetSibling = tempLeaf.getLeft();
		}
		else{
			targetSibling = tempLeaf.getRight();
		}
		int signal = whatToDo(tempLeaf,targetSibling);
		// The leaf is not underflowed.
		if (signal == 0){
			return;
		}
		// The leaf is underflowed, but the target sibling can spare some key/value pairs. Redistribute.
		if (signal == 1){
			K tempNewKey;
			if (isTargLeft){
				tempNewKey = redistributeLeaf(targetSibling, tempLeaf);
				int indexOfKey = tempParent.getChildrenArrayList().indexOf(tempLeaf);
				if (indexOfKey == 0){
					return;
				}
				tempParent.getKeyArrayList().set(indexOfKey - 1, tempNewKey);
				return;
			}
			else{
				tempNewKey = redistributeLeaf(tempLeaf,targetSibling);
				int indexOfKey = tempParent.getChildrenArrayList().indexOf(targetSibling);
				if (indexOfKey == 0){
					return;
				}
				tempParent.getKeyArrayList().set(indexOfKey - 1, tempNewKey);
				return;
			}
		}
		// The leaf is underflowed, and the target sibling cannot spare key/value pairs. Merge.
		if (signal == 2){
			if (isTargLeft){
				mergeLeaf(targetSibling,tempLeaf,tempParent);
			}
			else{
				mergeLeaf(tempLeaf,targetSibling,tempParent);
			}
			// If the parent is a root, the only time you do anything is if
			// the root is now empty. Then there should be only one child, so set that as root.
			if (tempPath.size() == 2){
				if (root.getKeyArrayList().size() == 0){
					root = tempLeaf;
				}
				return;
			}
			IndexNode<K,T> currentNode = null;
			// Redistribute/merge as necessary until the currentNode is not underflowed, or
			// you reach the root. The root can be underflowed, but not empty.
			for (int i = 1; i < tempPath.size() - 1; i++){
				// After the merge, the parent IndexNode may be underflowed.
				currentNode = (IndexNode<K, T>) tempPath.get(i);
				// Find the target sibling, according to the rules given in the instructions. There must
				// be a parent node because tempPath.size() > 2.
				IndexNode<K,T> parentNode = (IndexNode<K, T>) tempPath.get(i+1);
				IndexNode<K,T> targetIndexSibling = getTargetIndexSibling(currentNode,parentNode);
				int newSignal = whatToDo(currentNode, targetIndexSibling);
				// The current IndexNode is not underflowed.
				if (newSignal == 0){
					return;
				}
				// The current IndexNode is underflowed, but the target sibling can spare key/value pairs. Redistribute.
				boolean doLeft = isTargetIndexLeft(currentNode, parentNode);
				if (newSignal == 1){
					if (doLeft){
						redistributeIndexNode(targetIndexSibling, currentNode, parentNode);
					}
					else{
						redistributeIndexNode(currentNode,targetIndexSibling,parentNode);
					}
				}
				// The current IndexNode is underflowed, and the target sibling cannot spare key/value pairs. Merge.
				if (newSignal == 2){
					if (doLeft){
						mergeIndexNode(targetIndexSibling, currentNode, parentNode);
					}
					else{
						mergeIndexNode(currentNode, targetIndexSibling, parentNode);
					}
				}
			}
			if (((IndexNode<K, T>) root).getChildrenArrayList().size() <= 1){
				root = currentNode;
			}
		}
	}
	
	/**
	 * Redistributes keys and children between left node and right node while
	 * pushing through the splitting entry in the parent node.
	 * 
	 * @param leftNode
	 * @param rightNode
	 * @param parentNode
	 */
	private void redistributeIndexNode(IndexNode<K,T> leftNode, IndexNode<K,T> rightNode, IndexNode<K,T> parentNode){
		int indexOfBetween = parentNode.getChildrenArrayList().indexOf(leftNode);
		K betweenKey = parentNode.getKeyArrayList().get(indexOfBetween);
		ArrayList<K> allKeys = new ArrayList<K>();
		allKeys.addAll(leftNode.getKeyArrayList());
		allKeys.add(betweenKey);
		allKeys.addAll(rightNode.getKeyArrayList());
		ArrayList<Node<K,T>> allChildren = new ArrayList<Node<K,T>>();
		allChildren.addAll(leftNode.getChildrenArrayList());
		allChildren.addAll(rightNode.getChildrenArrayList());
		if (allKeys.size()%2 == 0){
			leftNode.setKeyArrayList(new ArrayList<K>(allKeys.subList(0, allKeys.size()/2)));
			rightNode.setKeyArrayList(new ArrayList<K>(allKeys.subList(allKeys.size()/2, allKeys.size())));
			leftNode.setChildrenArrayList(new ArrayList<Node<K, T>>(allChildren.subList(0, allKeys.size()/2)));
			rightNode.setChildrenArrayList(new ArrayList<Node<K, T>>(allChildren.subList(allKeys.size()/2, allKeys.size())));
		}
		else{
			leftNode.setKeyArrayList(new ArrayList<K>(allKeys.subList(0,allKeys.size()/2 + 1)));
			rightNode.setKeyArrayList(new ArrayList<K>(allKeys.subList(allKeys.size()/2 + 1, allKeys.size())));
			leftNode.setChildrenArrayList(new ArrayList<Node<K, T>>(allChildren.subList(0,allKeys.size()/2 + 1)));
			rightNode.setChildrenArrayList(new ArrayList<Node<K, T>>(allChildren.subList(allKeys.size()/2 + 1, allKeys.size())));
		}
		ArrayList<K> tempLeftKeys = leftNode.getKeyArrayList();
		K tempNewBetweenKey = tempLeftKeys.get(tempLeftKeys.size() - 1);
		parentNode.getKeyArrayList().set(indexOfBetween, tempNewBetweenKey);
		tempLeftKeys.remove(tempNewBetweenKey);
	}
	
	private void mergeIndexNode(IndexNode<K,T> leftNode, IndexNode<K,T> rightNode, IndexNode<K,T> parentNode){
		// Get the between key.
		int indexOfBetween = parentNode.getChildrenArrayList().indexOf(leftNode);
		K betweenKey = parentNode.getKeyArrayList().get(indexOfBetween);
		// Make a complete key array list and put it in the left node.
		ArrayList<K> allKeys = leftNode.getKeyArrayList();
		allKeys.add(betweenKey);
		allKeys.addAll(rightNode.getKeyArrayList());
		leftNode.setKeyArrayList(allKeys);
		// Make a complete children array list and put it in the left node.
		ArrayList<Node<K,T>> allChildren = leftNode.getChildrenArrayList();
		allChildren.addAll(rightNode.getChildrenArrayList());
		leftNode.setChildrenArrayList(allChildren);
		// Delete the pointer to the right child in the parent node.
		parentNode.getChildrenArrayList().remove(rightNode);
	}

	/**
	 * Helper method that detects whether the target sibling is the left
	 * or right sibling of the currentNode
	 * 
	 * @param currentNode
	 * @param parentNode
	 * @return true if the target sibling is the left sibling
	 */
	private boolean isTargetLeft(LeafNode<K,T> currentNode, IndexNode<K,T> parentNode){
		LeafNode<K,T> tempNode = currentNode.getLeft();
		if (tempNode == null || parentNode.getChildrenArrayList().indexOf(tempNode) == -1){
			return false;
		}
		return true;
	}
	
	private boolean isTargetIndexLeft(IndexNode<K,T> currentNode, IndexNode<K,T> parentNode){
		ArrayList<Node<K,T>> tempChildren = parentNode.getChildrenArrayList();
		int tempIndex = tempChildren.indexOf(currentNode);
		if (tempIndex == 0){
			return false;
		}
		else{
			return true;
		}
	}
	
	private IndexNode<K,T> getTargetIndexSibling(IndexNode<K,T> currentNode, IndexNode<K,T> parentNode){
		ArrayList<Node<K,T>> tempChildren = parentNode.getChildrenArrayList();
		int tempIndex = tempChildren.indexOf(currentNode);
		if (tempIndex == 0){
			return (IndexNode<K, T>) tempChildren.get(1);
		}
		else{
			return (IndexNode<K, T>) tempChildren.get(tempIndex - 1);
		}
	}
	
	/**
	 * Helper method that signals for redistribution or merging.
	 * 
	 * @param currentNode
	 * @param sibling is the target sibling
	 * @return 0 if there is nothing to do, 1 if you must redistribute,
	 * and 2 if you must merge
	 */
	private int whatToDo(Node<K,T> currentNode, Node<K,T> sibling){
		if (currentNode.getKeyArrayList().size() >= D){
			return 0;
		}
		else{
			if (sibling.getKeyArrayList().size() > D){
				return 1;
			}
			return 2;
		}
	}
	
	private void mergeLeaf(LeafNode<K,T> leftNode, LeafNode<K,T> rightNode, IndexNode<K,T> parentNode){
		leftNode.getKeyArrayList().addAll(rightNode.getKeyArrayList());
		leftNode.getValuesArrayList().addAll(rightNode.getValuesArrayList());
		leftNode.setRight(rightNode.getRight());
		rightNode.getRight().setLeft(leftNode);
		int indexOfRight = parentNode.getChildrenArrayList().indexOf(rightNode);
		parentNode.getChildrenArrayList().remove(indexOfRight);
		parentNode.getKeyArrayList().remove(indexOfRight - 1);
	}
	
	/**
	 * Helper method that redistributes keys and values between two
	 * leaf nodes. It adds all their keys and values in order into one
	 * ArrayList. Then it takes the first D elements and puts it into the
	 * left node, and puts the rest into the right node.
	 * 
	 * @param leftNode
	 * @param rightNode
	 * @return the smallest key of the right node, used to update
	 * the parent IndexNode's key ArrayList
	 */
	private K redistributeLeaf(LeafNode<K,T> leftNode, LeafNode<K,T> rightNode){
		ArrayList<T> tempValues = new ArrayList<T>();
		tempValues.addAll(leftNode.getValuesArrayList());
		tempValues.addAll(rightNode.getValuesArrayList());
		ArrayList<K> tempKeys = new ArrayList<K>();
		tempKeys.addAll(leftNode.getKeyArrayList());
		tempKeys.addAll(rightNode.getKeyArrayList());
		leftNode.setKeyArrayList(new ArrayList<K>(tempKeys.subList(0,tempKeys.size()/2)));
		rightNode.setKeyArrayList(new ArrayList<K>(tempKeys.subList(tempKeys.size()/2 + 1, tempKeys.size())));
		leftNode.setValuesArrayList(new ArrayList<T>(tempValues.subList(0, tempValues.size()/2)));
		rightNode.setValuesArrayList(new ArrayList<T>(tempValues.subList(tempValues.size()/2 + 1, tempValues.size())));
		return rightNode.getKeyArrayList().get(0);
	}
	
	/**
	 * Helper method to search through an index node and return
	 * the node that key should be in.
	 * 
	 * @param key is the key which we're searching for
	 * @param currentNode is the current node we're searching through
	 * @return Node pointed to by key
	 */
	private Node<K,T> searchThroughIndex(K key, IndexNode<K,T> currentNode){
		ArrayList<K> tempKeys = currentNode.getKeyArrayList();
		int lastIndex = tempKeys.size() - 1;
		if (key.compareTo(tempKeys.get(lastIndex)) >= 0){
			return currentNode.getChildrenArrayList().get(lastIndex + 1);
		}
		for (int i = 0; i < tempKeys.size(); i++){
			if (key.compareTo(tempKeys.get(i)) < 0){
				return currentNode.getChildrenArrayList().get(i);
			}
		}
		return currentNode.getChildrenArrayList().get(0);
	}
	
	/**
	 * Helper method to search through a leaf node and return
	 * the value, if there is any, assigned to the key.
	 * 
	 * @param key is the key which we're searching for
	 * @param currentNode is the leaf node we're searching through
	 * @return value, if there is any, assigned to this key
	 */
	private T searchThroughLeaf(K key, LeafNode<K,T> currentNode){
		int temp = currentNode.getKeyArrayList().indexOf(key);
		if (temp != -1){
			return currentNode.getValuesArrayList().get(temp);
		}
		return null;
	}

	/**
	 * Returns the path from root to LeafNode containing key.
	 * 
	 * @param key is the key we're adding
	 * @return
	 */
	private ArrayList<Node<K,T>> searchPathOfKey(K key){
		ArrayList<Node<K,T>> pathArrayList = new ArrayList<Node<K,T>>();
		pathArrayList.add(root);
		Node<K,T> currentNode = root;
		while (!(currentNode.isLeafNode)){
			currentNode = searchThroughIndex(key, (IndexNode<K, T>) currentNode);
			pathArrayList.add(currentNode);
		}
		Collections.reverse(pathArrayList);
		return pathArrayList;
	}

}
