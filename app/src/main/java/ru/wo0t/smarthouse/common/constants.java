package ru.wo0t.smarthouse.common;

/**
 * Created by alex on 2/5/15.
 */
public interface constants {
    public static final double version = 1.0;

    public static final String APP_TAG = "smhz";

    public static final String LOCAL_BOARD_KEYWORD = "smhz";
    public static final String REMOTE_BOARD_URL_STRING = "http://dacha.wo0t.ru/remote.php";
    public static final String REMOTE_BOARD_USER = "admin";
    public static final String REMOTE_BOARD_PASSWORD = "asdfq1";

    public static final String SENS_VAL = "val";
    public static final String SENS_TYPE = "type";
    public static final String SENS_ADDR = "addr";
    public static final String SENS_NAME = "name";
    public static final String SENS_ERR_SIGN = "err_sign";
    public static final String SENS_ERR_VAL = "err_val";
    public static final String SENS_ERR_WARN = "err_warn";

    public static final int LOCAL_BOARD_PORT = 1352;
    public static final int BOARD_LOOKUP_TIMEOUT = 5000;
    public static final int LOCAL_BOARD_RECONNECT_TIME = 5000;
    public static final int REMOTE_BOARD_WAIT_PERIOD = 10000;
}
