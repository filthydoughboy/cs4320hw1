import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class LeafNode<K extends Comparable<K>, T> extends Node<K, T> {
	protected ArrayList<T> values;
  protected LeafNode<K,T> nextLeaf;
  protected LeafNode<K,T> previousLeaf;

	public LeafNode(K firstKey, T firstValue) {
		isLeafNode = true;
		keys = new ArrayList<K>();
		values = new ArrayList<T>();
		keys.add(firstKey);
		values.add(firstValue);
		nextLeaf = null;
		previousLeaf = null;

	}

	public LeafNode(List<K> newKeys, List<T> newValues) {
		isLeafNode = true;
		keys = new ArrayList<K>(newKeys);
		values = new ArrayList<T>(newValues);

	}

	/**
	 * insert key/value into this node so that it still remains sorted
	 * 
	 * @param key
	 * @param value
	 */
	public void insertSorted(K key, T value) {
		if (key.compareTo(keys.get(0)) < 0) {
			keys.add(0, key);
			values.add(0, value);
		} else if (key.compareTo(keys.get(keys.size() - 1)) > 0) {
			keys.add(key);
			values.add(value);
		} else {
			ListIterator<K> iterator = keys.listIterator();
			while (iterator.hasNext()) {
				if (iterator.next().compareTo(key) > 0) {
					int position = iterator.previousIndex();
					keys.add(position, key);
					values.add(position, value);
					break;
				}
			}

		}
	}
	
	public ArrayList<K> getKeyArrayList(){
		return keys;
	}
	
	public ArrayList<T> getValuesArrayList(){
		return values;
	}
	
	public void setKeyArrayList(ArrayList<K> arg0){
		keys = arg0;
	}
	
	public void setValuesArrayList(ArrayList<T> arg0){
		values = arg0;
	}
	
	public LeafNode<K,T> getRight(){
		return nextLeaf;
	}
	
	public LeafNode<K,T> getLeft(){
		return previousLeaf;
	}
	
	public void setRight(LeafNode<K,T> arg0){
		nextLeaf = arg0;
	}
	
	public void setLeft(LeafNode<K,T> arg0){
		previousLeaf = arg0;
	}

	public void removeKey(K key){
		int temp = keys.indexOf(key);
		values.remove(temp);
		keys.remove(temp);
	}
	
}
