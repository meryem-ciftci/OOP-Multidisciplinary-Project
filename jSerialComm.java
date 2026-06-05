import com.fazecast.jSerialComm.SerialPort;

public class AudiometerSerialManager{
    private static SerialPort comPort;
    private static PrintWriter output;
    private static Thread readThread;
    private static boolean isRunning = false;
    private static AudiometerListener listener;

    public interface AudiometerListener{
        void onPatientResponseReceived();
        void onAckReceived(String message);
    }

    public AudiometerSerialManager(AudiometerListener listener){
        this.listener=listener;
    }

    public void connect(String portName){
        comPort = SerialPort.getCommPort(portName);
        comPort.setComPortParameters(9600,8,SerialPort.ONE_STOP_BIT,SerialPort.NO_PARITY);

        if (comPort.openPort()){
            System.out.println("Successfully opened it yeyy"+portName);
            output = new PrintWriter(comPort.getOutPutStream(),true);
            startReading();
        }else{
            System.err.println("Failed...")
        }
    }

    public void sendCommand(String command){
        if (output){
            output.print(command+"\n"){
                output.flushIOBuffers();
            }
        }
    }

    public void changeFreqAndAmp(double Freq,int Amp){
        sendCommand(Freq,Amp);
    }

    private void startReading(){
        Scanner scanner = new Scanner();
        InputStream input = scanner(comport.getInputStream());
        isRunning = true;
        readThread = new Thread(()->{
            if (input){
                while(isRunning && scanner.hasNextLine()){
                    String line = scanner.nextLine().trim():

                    if (line.equals("RESPONSE")){
                        if(listener){
                            listener.onPatientResponseReceived();
                        }
                    } else if(line.startsWith("ACK:")) || line.startsWith("NAK:") || line.equals("READY") {
                        if(listener){
                            listener.onAckReceived(line);
                        }
                    }
                }
            } else{
                System.err.println("Errorrrr");
            }
        });
        readThread.start();
    }

    public void disconnect(){
        isRunning=false;
        if(comPort!=null && comPort.isOpenPort()){
            comPort.closePort();
        }
    }
}