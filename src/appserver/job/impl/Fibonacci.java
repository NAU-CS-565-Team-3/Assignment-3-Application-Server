/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appserver.job.impl;

import appserver.job.Tool;

/**
 * Fibonacci class gets the Fibonacci result based on the sequence number by calling FibonacciHelper
 * @author Jacob Kaufman
 */
public class Fibonacci implements Tool{

    FibonacciHelper helper = null;
    
    @Override
    public Object go(Object parameters) {
        
        helper = new FibonacciHelper((Integer) parameters);
        return helper.getResult();
    }
}
