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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.sintef.jarduino.comm.AndroidBluetooth4JArduino;
import org.sintef.jarduino.observer.JArduinoClientObserver;
import org.sintef.jarduino.observer.JArduinoClientSubject;
import org.sintef.jarduino.observer.JArduinoObserver;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

public class GUIController implements JArduinoObserver, JArduinoClientSubject {

    private List<JArduinoClientObserver> handlers;
    private SimpleDateFormat dateFormat;
    private JArduino mJArduino;
    private String TAG = "GUIController";
    private ListView logList;
    private Activity mActivity;

    public GUIController(ListView logger, Activity activity){
        this.logList = logger;
        mActivity = activity;
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
            Log.d(TAG, data+" --> "+data.getPacket());
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
        doSend(fsp);
    }

    public final void senddigitalRead(DigitalPin pin) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createDigitalRead(pin);
        doSend(fsp);
    }

    public final void senddigitalWrite(DigitalPin pin, DigitalState value) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createDigitalWrite(pin, value);
        doSend(fsp);
    }

    public final void sendanalogRead(AnalogPin pin) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createAnalogRead(pin);
        doSend(fsp);
    }

    public final void sendanalogWrite(PWMPin pin, byte value) {
        FixedSizePacket fsp = null;
        fsp = JArduinoProtocol.createAnalogWrite(pin, value);
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
