package com.empanak.dmojutils.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

//Hay que ver como hacer para que no se muestre la clase de donde proviene el log

//Hay que implementar el logger en todo el proyecto

public class LoggerConfig extends FileHandler {
    private static final String LOG_DIR = System.getProperty("user.home") + "/.dmojutils";
    private static final String LOG_FILE = LOG_DIR + "/latest.log";

    private static String ensureLogFile() throws IOException {
        File dir = new File(LOG_DIR);

        if(!dir.exists() && !dir.mkdirs()){
            throw new IOException("No se pudo crear el directorio de logs: " + dir);
        }
        return  LOG_FILE;
    }

    public LoggerConfig() throws IOException {
        super(ensureLogFile(), true);
        setFormatter(new SimpleFormatter());
    }

    @Override
    public void publish(LogRecord record) {
        if(!isLoggable(record))
            return;

        try{
            super.publish(record);
            flush();
        }catch (Exception e){
            getErrorManager().error("Error al escribir el log", e, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush(){
        try{
            super.flush();
        }catch (Exception e){
            getErrorManager().error("Error al guardar el log", e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public void close() throws SecurityException{
        try{
            super.close();
        }catch (Exception e){
            getErrorManager().error("Error al cerrar el log", e, ErrorManager.CLOSE_FAILURE);
        }
    }
}
