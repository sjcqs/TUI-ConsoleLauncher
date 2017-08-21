package ohi.andre.consolelauncher.commands;

public interface CommandAbstraction {

    //	undefinied n of arguments
    int UNDEFINIED = -1;

    //	arg type
    int PLAIN_TEXT = 10;
    int FILE = 11;
    int VISIBLE_PACKAGE = 12;
    int CONTACTNUMBER = 13;
    int TEXTLIST = 14;
    int SONG = 15;
    int FILE_LIST = 16;
    int COMMAND = 17;
    int PARAM = 18;
    int BOOLEAN = 19;
    int HIDDEN_PACKAGE = 20;
    int COLOR = 21;
    int CONFIG_FILE = 22;
    int CONFIG_ENTRY = 23;
    int INT = 24;
    int DEFAULT_APP = 25;
    int ALL_PACKAGES = 26;
    int WHATSAPPNUMBER = 27;

    String exec(ExecutePack pack) throws Exception;

    int minArgs();

    int maxArgs();

    int[] argType();

    int priority();

    int helpRes();

    String onArgNotFound(ExecutePack pack, int indexNotFound);

    String onNotArgEnough(ExecutePack pack, int nArgs);
}
