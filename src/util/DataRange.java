package util;

import java.util.ArrayList;
import java.util.List;

public class DataRange {
    
    
    
    public static List<int[]> findOverLap(int[][] a , int[][] b){
        List<int[]> list = new ArrayList<int[]>();
        int i=0;
        int j=0;
     
        while(i<a.length && j<b.length){
      
            int maxX = a[i][0]>=b[j][0]?a[i][0]:b[j][0];
            int minY = a[i][1]<=b[j][1]?a[i][1]:b[j][1];
      
            if(maxX <= minY){
                int[] temp = new int[]{maxX,minY};
                list.add(temp);
            }
            
            if(a[i][1] <= b[j][1]) {
                i++;
            } else {
                j++;
            }
        }
     
        return list;
    }

}
