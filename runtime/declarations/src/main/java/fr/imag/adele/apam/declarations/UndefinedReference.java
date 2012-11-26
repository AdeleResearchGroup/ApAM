package fr.imag.adele.apam.declarations;

public class UndefinedReference extends ResourceReference {

    private final String subject;
    
    private final Class<?> kind;

    public UndefinedReference(String subject,Class<?> kind){
        super("<Unavailable>");
        this.subject = subject;
        this.kind= kind;
    }
    
    public Class<?> getKind(){
        return kind;
    }
   
    public String getSubject(){
        return subject;
    }
       
    public String toString() {
        return "resource UNKNOWN";
    }
}
