


public final class PConfError {

    public static PConfError getInstance() {
        if (pInstance == null) {
            pInstance = new PConfError();
        }
        
        return pInstance;
    }
    
    public void addMessage(String msg) {
        errMsg.append(msg);
    }
    
    public void exit(int errCode) {
        System.err.print(errMsg);
        System.exit (-1);
    }
    
    public void checkError() {
        if (hasErrorMsg()) {
            exit(-1);
        }
    }
    
    boolean hasErrorMsg() {
        return (errMsg.length() > 0);
    }
    
    
    private PConfError ()   {}
    
    
    private static PConfError pInstance = null;
    
    private static StringBuffer errMsg= new StringBuffer();
}
