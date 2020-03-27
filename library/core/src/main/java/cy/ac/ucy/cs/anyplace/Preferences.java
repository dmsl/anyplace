package cy.ac.ucy.cs.anyplace;

public class Preferences {

    // Private variable and through a static method i change the values.

    // CA: all preference related operations should come from here.
    // make them private, setting, getting, reading from the file, etc.

    public static int CONNECT_TIMEOUT_SECS = 10;

    // 30 seconds is too high.. even 10 might be..
    // We'll revise this at some point. and have some defaults here,
    // which will be initialized in the file,
    public static int READ_TIMEOUT_SECS = 30;
}
