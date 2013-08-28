/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Jan Ole Skotterud Franck Fleurey and Brice Morin
 * Company: SINTEF IKT, Oslo, Norway
 * Date: 2011
 */
package org.sintef.jarduino;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import org.sintef.jarduino.comm.AndroidBluetooth4JArduino;
import org.sintef.jarduino.observer.JArduinoClientObserver;
import org.sintef.jarduino.observer.JArduinoClientSubject;
import org.sintef.jarduino.observer.JArduinoObserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GUIController implements JArduinoObserver, JArduinoClientSubject {

    private List<JArduinoClientObserver> handlers;
    private List<LogObject> orders;
    private SimpleDateFormat dateFormat;
    private JArduino mJArduino;
    private String TAG = "GUIController";
    private ListView logList;
    private Activity mActivity;
    private CheckBox mSave;

    public GUIController(ListView logger, Activity activity, CheckBox box){
        orders = new ArrayList<LogObject>();
        this.logList = logger;
        mActivity = activity;
        mSave = box;
        handlers = new LinkedList<JArduinoClientObserver>();
        dateFormat = new SimpleDateFormat("dd MMM yyy 'at' HH:mm:ss.SSS");
    }

    private void addToLogger(String s){
        class OneShotTask implements Runnable {
            String str;
            OneShotTask(String s) { str = s; }
            public void run() {
                ((ArrayAdapter)logList.getAdapter()).add(str);
                logList.invalidate();
                logList.setSelection(logList.getCount());
            }
        }
        mActivity.runOnUiThread(new OneShotTask(s));
    }

    private void doSend(FixedSizePacket data){
        if (data != null) {
            Log.d(TAG, data + " --> " + data.getPacket());
            addToLogger(data.toString());
            for (JArduinoClientObserver h : handlers){
                h.receiveMsg(data.getPacket());
            }
        }
        else {
            Log.d(TAG, "Data is null");
        }
    }

    public final void sendpinMode(PinMode mode, DigitalPin pin) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createPinMode(pin, mode);
        if(mSave.isChecked()){
            if(mode == PinMode.INPUT)
                orders.add(new LogDigitalObject(pin, "input", (short)-1, (short)-1, (byte)-1));
            else
                orders.add(new LogDigitalObject(pin, "output", (short) -1, (short) -1, (byte) -1));
        }
        doSend(fsp);
    }

    public void toFile(){
        Log.d(TAG, "toFile");
        FileOutputStream output = null;
        String filename = ".saved";

        try {
            output = mActivity.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(output == null){
            Log.d(TAG, "open issue");
            return;
        }
        for(LogObject o : orders){
            if(o instanceof LogDigitalObject){
                LogDigitalObject digital =(LogDigitalObject) o;

                try {
                    output.write(String.valueOf("Digital[" + digital.getPin() + "," +
                            digital.getAddr() + "," +
                            digital.getB() + "," +
                            digital.getMode() + "," +
                            digital.getVal() + "]").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if(o instanceof LogAnalogObject){
                LogAnalogObject analog =(LogAnalogObject) o;

                try {
                    output.write(String.valueOf("Analog[" + analog.getPin() + "," +
                            analog.getAddr() + "," +
                            analog.getB() + "," +
                            analog.getMode() + "," +
                            analog.getVal() + "]").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(o instanceof LogPWMObject){
                LogPWMObject pwm =(LogPWMObject) o;

                try {
                    output.write(String.valueOf("Pwm[" + pwm.getPin() + "," +
                            pwm.getAddr() + "," +
                            pwm.getB() + "," +
                            pwm.getMode() + "," +
                            pwm.getVal() + "]").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWord(FileInputStream in, char separator){
        int b;
        String word = "";
        try {
            while((b = in.read()) != separator){
                word += b;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return word;
    }

    public void fromFile(){

        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[1024];




        Log.d(TAG, "fromFile");
        FileInputStream input = null;
        String filename = ".saved";

        orders.clear();

        try {
            input = mActivity.openFileInput(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(input == null){
            Log.d(TAG, "open issue");
            return;
        }

        try {
            while (input.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        char c;
        String word = "";
        int pointer = 0;

        Log.d(TAG, fileContent.toString());

        while(pointer < fileContent.lastIndexOf("]")){
            word = fileContent.substring(pointer);
            Log.d(TAG, word);
            word = word.substring(0, word.indexOf("["));
            Log.d(TAG, word);
            if(word.equals("Digital")){
                pointer += fileContent.substring(pointer).indexOf("[")+1;
                int finalPointer = fileContent.substring(pointer).indexOf("]") + pointer;
                String object = fileContent.substring(pointer, finalPointer);
                finalPointer ++;

                String data[] = object.split(",");
                LogDigitalObject digital = new LogDigitalObject();

                digital.setPin(DigitalPin.A_0.valueOf(data[0]));
                digital.setAddr(Short.parseShort(data[1]));
                digital.setB(Byte.parseByte(data[2]));
                digital.setMode(data[3]);
                digital.setVal(Short.parseShort(data[4]));
                orders.add(digital);
                pointer = finalPointer;
            }
            if(word.equals("Analog")){
                pointer += fileContent.substring(pointer).indexOf("[")+1;
                int finalPointer = fileContent.substring(pointer).indexOf("]") + pointer;
                String object = fileContent.substring(pointer, finalPointer);
                finalPointer ++;

                String data[] = object.split(",");
                LogAnalogObject analog = new LogAnalogObject();

                analog.setPin(AnalogPin.valueOf(data[0]));
                analog.setAddr(Short.parseShort(data[1]));
                analog.setB(Byte.parseByte(data[2]));
                analog.setMode(data[3]);
                analog.setVal(Short.parseShort(data[4]));
                orders.add(analog);
                pointer = finalPointer;
            }
            if(word.equals("Pwm")){
                pointer += fileContent.substring(pointer).indexOf("[")+1;
                int finalPointer = fileContent.substring(pointer).indexOf("]") + pointer;
                String object = fileContent.substring(pointer, finalPointer);
                finalPointer ++;

                String data[] = object.split(",");
                LogPWMObject pwm = new LogPWMObject();

                pwm.setPin(PWMPin.valueOf(data[0]));
                pwm.setAddr(Short.parseShort(data[1]));
                pwm.setB(Byte.parseByte(data[2]));
                pwm.setMode(data[3]);
                pwm.setVal(Short.parseShort(data[4]));
                orders.add(pwm);
                pointer = finalPointer;
            }
        }

        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void senddigitalRead(DigitalPin pin) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createDigitalRead(pin);
        if(mSave.isChecked())
            orders.add(new LogDigitalObject(pin, "digitalRead", (short)-1, (short)-1, (byte)-1));
        doSend(fsp);
    }

    public final void senddigitalWrite(DigitalPin pin, DigitalState value) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createDigitalWrite(pin, value);
        if(mSave.isChecked()){
            if(value == DigitalState.HIGH)
                orders.add(new LogDigitalObject(pin, "high", (short)-1, (short)-1, (byte)-1));
            else
                orders.add(new LogDigitalObject(pin, "low", (short) -1, (short) -1, (byte) -1));
        }
        doSend(fsp);
    }

    public final void sendanalogRead(AnalogPin pin) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createAnalogRead(pin);
        if(mSave.isChecked())
            orders.add(new LogAnalogObject(pin, "analogRead", (short)-1, (short)-1, (byte)-1));
        doSend(fsp);
    }

    public final void sendanalogWrite(PWMPin pin, byte value) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createAnalogWrite(pin, value);
        if(mSave.isChecked())
            orders.add(new LogPWMObject(pin, "analogWrite", (short)-1, (short)value, (byte)-1));
        doSend(fsp);
    }

    public final void sendping() {
        doSend(JArduinoProtocol.createPing());
    }

    public final void receiveMessage(byte[] packet){
        FixedSizePacket data = JArduinoProtocol.createMessageFromPacket(packet);
        if (data != null) {
            //gui.writeToLog( " ["+dateFormat.format(new Date(System.currentTimeMillis()))+"]: "+data.toString()+" --> "+FixedSizePacket.toString(packet));
            Log.d(TAG, /*" [" + dateFormat.format(new Date(System.currentTimeMillis())) + "]: " +*/ data.toString() /*+ " --> " + FixedSizePacket.toString(packet)*/);
            addToLogger(data.toString());
            //TODO Add
        }
    }

    public void executeOrders(){
        new CommandExecuter(this, new ArrayList<LogObject>(orders)).run();
    }

    //Methods defined in the Observer pattern specific to JArduino
    public void receiveMsg(byte[] msg) {
        receiveMessage(msg);
    }

    public void register(JArduinoClientObserver observer) {
        handlers.add(observer);
    }

    public void unregister(JArduinoClientObserver observer) {
        handlers.remove(observer);
    }

    public void unregisterAll(){
        AndroidBluetooth4JArduino temp;
        for (int i = 0; i < handlers.size(); i++){
            temp = (AndroidBluetooth4JArduino) handlers.get(i);
            Log.d(TAG, "Closer " + temp);
            //temp.close();
        }
        handlers.clear();
        Log.d(TAG, "Size = " + handlers.size());
    }
}
