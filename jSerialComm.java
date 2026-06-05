import com.fazecast.jSerialComm.SerialPort; 
import java.io.InputStream; 
import java.io.PrintWriter; 
import java.util.Scanner; 

  

public class AudiometerSerialManager { 
    private static SerialPort comPort; 
    private static PrintWriter output; 
    private static Thread readThread; 
    private static boolean isRunning = false; 
    private static AudiometerListener listener; 

  

    public interface AudiometerListener { 
        void onPatientResponseReceived(); 
        void onAckReceived(String message); 
    } 

  

    public AudiometerSerialManager(AudiometerListener listener) { 
        this.listener = listener; 
    } 

  

    public void connect(String portName) { 
        comPort = SerialPort.getCommPort(portName); 
        comPort.setComPortParameters(9600,8,SerialPort.ONE_STOP_BIT,SerialPort.NO_PARITY); 

         

        if (comPort.openPort()) { 
            System.out.println("Successfully opened it yeyy"+portName); 
            output = new PrintWriter(comPort.getOutputStream(),true); 
            startReading(); 
        } else { 
            System.err.println("Failed..."); 
        } 

    } 

  

    public void sendCommand(String command) { 
        if (output != null) { 
            output.print(command+"\n"); 
            output.flush(); 
        } 

    } 

  

    public void changeFreqAndAmp(double Freq,int Amp) { 
        sendCommand("FREQ:" + Freq + ",AMP:" + Amp); 
    } 

  

    private void startReading() { 
        isRunning = true; 
        readThread = new Thread(()->{ 
            Scanner scanner = new Scanner(comPort.getInputStream()); 
            if (scanner != null) { 
                while(isRunning && scanner.hasNextLine()) { 
                    String line = scanner.nextLine().trim(); 
                    if (line.equals("RESPONSE")) { 
                        if(listener != null) { 
                            listener.onPatientResponseReceived(); 
                        } 
                    } else if(line.startsWith("ACK:") || line.startsWith("NAK:") || line.equals("READY")) { 
                        if(listener != null) { 
                            listener.onAckReceived(line); 
                        } 
                    } 
                } 
            } else { 
                System.err.println("Errorrrr"); 
            } 

        }); 
        readThread.start(); 
    } 

  

    public void disconnect() { 
        isRunning = false; 
        if(comPort!=null && comPort.isOpenPort()) { 
            comPort.closePort(); 
        } 

    } 

} 