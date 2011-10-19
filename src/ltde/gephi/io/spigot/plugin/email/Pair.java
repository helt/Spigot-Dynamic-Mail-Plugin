/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email;

/**
 *
 * @author Hendrik
 */
class Pair<K,E> {

    public K  key;
    public E element;
    
    public Pair(K k, E e){
        this.key=k;
        this.element=e;
    }

}
