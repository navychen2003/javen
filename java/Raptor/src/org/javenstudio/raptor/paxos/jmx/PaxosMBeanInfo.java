package org.javenstudio.raptor.paxos.jmx;

/**
 * Paxos MBean info interface. MBeanRegistry uses the interface to generate
 * JMX object name.
 */
public interface PaxosMBeanInfo {
    /**
     * @return a string identifying the MBean 
     */
    public String getName();
    /**
     * If isHidden returns true, the MBean won't be registered with MBean server,
     * and thus won't be available for management tools. Used for grouping MBeans.
     * @return true if the MBean is hidden.
     */
    public boolean isHidden();
}
