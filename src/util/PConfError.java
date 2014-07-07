package util;



public final class PConfError {

    public static PConfError getInstance() {
        if (pInstance == null) {
            pInstance = new PConfError();
        }
        
        return pInstance;
    }
    
    public void addMessage(String msg) {
        errMsg.append(msg);
        errMsg.append("\n");
    }
    
    public void checkError() {
        if (hasErrorMsg()) {
            System.err.print(errMsg);
            System.exit (-1);
            //exit(-1);
        }
    }
    
    public boolean hasErrorMsg() {
        return (errMsg.length() > 0);
    }
    
    
    private PConfError ()   {}
    
    
    private static PConfError pInstance = null;
    
    private static StringBuffer errMsg= new StringBuffer();
}
