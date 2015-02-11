package ru.wo0t.smarthouse.common;

/**
 * Created by alex on 2/5/15.
 */
public interface constants {
    public static final double version = 1.0;

    public static final int MESSAGE_NEW_BOARD = 1;
    public static final int MESSAGE_DISCOVERY_FINISHED = 2;
    public static final int MESSAGE_CONNECTED = 3;
    public static final int MESSAGE_DISCONNECTED = 4;
    public static final int MESSAGE_NEW_MSG = 5;

    public static final String MESSAGE_INFO = "msg_info";
    public static final String MESSAGE_DATA = "msg_data";
    public static final String APP_TAG = "smhz";

    public static final String BOARD_KEYWORD = "smhz";


    public static final String SENS_VAL = "val";
    public static final String SENS_TYPE = "type";
    public static final String SENS_ADDR = "addr";
    public static final String SENS_NAME = "name";
    public static final String SENS_ERR_SIGN = "err_sign";
    public static final String SENS_ERR_VAL = "err_val";
    public static final String SENS_ERR_WARN = "err_warn";

    public static final int REMOTE_BOARD_PORT = 1352;
}
