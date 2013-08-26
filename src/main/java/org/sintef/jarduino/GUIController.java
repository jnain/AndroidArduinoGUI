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

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
    private LinearLayout logger;

    public GUIController(LinearLayout logger){
        this.logger = logger;
        this.logger.setVerticalScrollBarEnabled(true);
        handlers = new LinkedList<JArduinoClientObserver>();
        dateFormat = new SimpleDateFormat("dd MMM yyy 'at' HH:mm:ss.SSS");
    }

    private void addToLogger(String s){
        TextView tv = createTextView(s);
        logger.addView(tv);  // block here one the addView dunno why. (not in createTextView)
        ((ScrollView)logger.getParent()).fullScroll(View.FOCUS_DOWN);
    }

    private TextView createTextView(String s){
        TextView t = new TextView(logger.getContext());
        t.setTextSize(12);
        t.setTextColor(Color.BLACK);
        t.setText(s);
        /*LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        t.setLayoutParams(params);*/
        return t;
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
