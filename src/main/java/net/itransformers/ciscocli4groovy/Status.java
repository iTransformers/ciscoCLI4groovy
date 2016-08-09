package net.itransformers.ciscocli4groovy;

/**
 * Created by niau on 8/8/16.
 */
public enum Status {
    success(1), failure(2), timeout(3);
    private int status;
    private Status (int status){
        this.status = status;
    }

}
