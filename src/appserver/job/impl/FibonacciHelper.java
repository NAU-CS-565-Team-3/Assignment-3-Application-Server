/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appserver.job.impl;

/**
 *
 * @author Jacob Kaufman
 */
public class FibonacciHelper {
    
    Integer sequenceNumber = null;
    
    public FibonacciHelper(Integer number) {
        this.sequenceNumber = number;
    }
    
    public long getResult() {
        
       long num1 = 0, num2 = 1;
       
       // return for base cases
       if (sequenceNumber == 1 | sequenceNumber == 0){
           return 1;
       }
  
       int counter = 0;
  
        // Iterate till counter is sequence Number
        while (counter < sequenceNumber) {
            // calculate next sequence number
            long nextSequenceNumber = num2 + num1;
            
            //swap numbers
            num1 = num2;
            num2 = nextSequenceNumber;
            counter = counter + 1;
        }
        
        return num2;
    }
}
