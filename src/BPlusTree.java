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
		
		ArrayList<Node<K,T>> tempPath = searchPathOfKey(key);
		LeafNode<K,T> tempLeaf = (LeafNode<K, T>) tempPath.get(0);
		tempLeaf.insertSorted(key,value);
		Entry<K, Node<K,T>> tempNewEntry = null;
		if (tempLeaf.getKeyArrayList().size() > 2*D){
			tempNewEntry = splitLeafNode(tempLeaf);
			IndexNode<K,T> tempIndexNode = (IndexNode<K,T>) tempPath.get(1);
			int theIndex = tempIndexNode.getChildrenArrayList().indexOf(tempLeaf);
			tempIndexNode.insertSorted(tempNewEntry,theIndex);
		}
		else{
			return;
		}
		for (int i = 1; i < tempPath.size() - 1; i++){
			IndexNode<K,T> tempNode = (IndexNode<K, T>) tempPath.get(i);
			Node<K,T> properChild = tempPath.get(i-1);
			int theIndex = tempNode.getChildrenArrayList().indexOf(properChild);
			tempNode.insertSorted(tempNewEntry, theIndex);
			if (tempNode.getChildrenArrayList().size() <= 2*D){
				return;
			}
			tempNewEntry = splitIndexNode(tempNode);
		}
		Node<K,T> tempLeftChild = tempPath.get(tempPath.size() - 1);
		if (tempLeftChild.getKeyArrayList().size() <= 2*D){
			return;
		}
		if (tempLeftChild.isLeafNode){
			tempNewEntry = splitLeafNode((LeafNode<K, T>) tempLeftChild);
		}
		else{
			tempNewEntry = splitIndexNode((IndexNode<K, T>) tempLeftChild);
		}
		K tempRootKey = tempNewEntry.getKey();
		Node<K,T> tempRightChild =  tempNewEntry.getValue();
		root = new IndexNode<K,T>(tempRootKey,tempLeftChild,tempRightChild);
	}

	/**
	 * Splits an overflowed LeafNode into two Nodes.
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
		for (int i = D; i < 2*D + 1; i++){
			tempNewKeys.add(tempKeys.get(i));
			tempNewValues.add(tempValues.get(i));
		}
		for (int i = D; i < 2*D + 1; i++){
			tempKeys.remove(D);
			tempValues.remove(D);
		}
		LeafNode<K,T> tempLeaf = new LeafNode<K,T>(tempNewKeys,tempNewValues);
		K firstKey = tempLeaf.getKeyArrayList().get(0);
		Entry<K, Node<K,T>> tempEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(firstKey,tempLeaf);
		return tempEntry;
	}

	/**
	 * Splits an overflowed IndexNode into two Nodes.
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
		System.out.println(tempChildren);
		tempNewKeys.add(tempKeys.get(D));
		for (int i = D + 1; i < 2*D + 1; i++){
			tempNewKeys.add(tempKeys.get(i));
			tempNewChildren.add(tempChildren.get(i));
		}
		tempNewChildren.add(tempChildren.get(2*D + 1));
		for (int i = D; i < 2*D + 1; i++){
			tempKeys.remove(D);
			tempChildren.remove(D + 1);
		}
		System.out.println(tempChildren);
		System.out.println(tempKeys);
		System.out.println(tempNewChildren);
		System.out.println(tempNewKeys);
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

	}

	/**
	 * TODO Handle LeafNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
			IndexNode<K,T> parent) {
		return -1;

	}

	/**
	 * TODO Handle IndexNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
			IndexNode<K,T> rightIndex, IndexNode<K,T> parent) {
		return -1;
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
		if (key.compareTo(tempKeys.get(lastIndex)) > 0){
			return currentNode.getChildrenArrayList().get(lastIndex + 1);
		}
		for (int i = 0; i < tempKeys.size(); i++){
			if (key.compareTo(tempKeys.get(i)) <= 0){
				return currentNode.getChildrenArrayList().get(i);
			}
		}
		return null;
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
