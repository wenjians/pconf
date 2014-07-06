
package util;

import java.util.ArrayList;
import java.util.List;

public class DataRange {
   
	String range;
	List <int[]> rangList;
	
    DataRange(String _range) {
        rangList = null;
        
    	this.range = _range;
    	if (this.range == null) {
    	    return;
    	}
    	
    	rangList = new ArrayList <int[]> ();

    	String[] strRangeList = _range.split("(\\|)");
        for (String oneRange: strRangeList) {
            String[] oneRangList = oneRange.split("(\\.\\.)");
            int[] oneRangArray;
            if (oneRangList.length == 1) {
                oneRangArray = new int[2];
                oneRangArray[0] = Integer.valueOf(oneRangList[0]);
                oneRangArray[1] = Integer.valueOf(oneRangList[0]);
            } else if (oneRangList.length == 2) {
                oneRangArray = new int[2];
                oneRangArray[0] = Integer.valueOf(oneRangList[0]);
                oneRangArray[1] = Integer.valueOf(oneRangList[1]);
            } else {
                break;
            }
            rangList.add(oneRangArray);
        }
    }
    
    int getMinVlaue() {
        if (rangList == null)
            return 0;
        
        return rangList.get(0)[0];
    }
    
    int getMaxVlaue() {
        if (rangList == null)
            return 0;
        
        return rangList.get(rangList.size()-1)[1];
    }
    
    
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
