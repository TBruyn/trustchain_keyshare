package nl.tudelft.cs4160.trustchain_android.main;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.appToApp.PeerAppToApp;
import nl.tudelft.cs4160.trustchain_android.chainExplorer.ChainExplorerActivity;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.connection.network.NetworkCommunication;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.keyrestore.KeyShareActivity;


public class TrustChainActivity extends AppCompatActivity implements CommunicationListener {


    public final static String TRANSACTION = "Hello world!";
    private final static String TAG = TrustChainActivity.class.toString();

    TrustChainDBHelper dbHelper;

    TextView externalIPText;
    TextView localIPText;
    TextView statusText;
    Button connectionButton;
    EditText editTextDestinationIP;
    EditText editTextDestinationPort;

    TrustChainActivity thisActivity;
    PeerAppToApp peerAppToApp;
    Peer peer;

    private Communication communication;

    /**
     * Key pair of user
     */
    static KeyPair kp;

    /**
     * Listener for the connection button.
     * On click a block is created and send to a peerAppToApp.
     * When we encounter an unknown peerAppToApp, send a crawl request to that peerAppToApp in order to get its
     * public key.
     * Also, when we want to send a block always send our last 5 blocks to the peerAppToApp so the block
     * request won't be rejected due to NO_INFO error.
     * <p>
     * This is code to simulate dispersy, note that this does not work properly with a busy network,
     * because the time delay between sending information to the peerAppToApp and sending the actual
     * to-be-signed block could cause gaps.
     * <p>
     * Also note that whatever goes wrong we will never get a valid full block, so the integrity of
     * the network is not compromised due to not using dispersy.
     */

    public void onClickConnect(View view) {
        peer = new Peer(null, editTextDestinationIP.getText().toString(),
                Integer.parseInt(editTextDestinationPort.getText().toString()));
        communication.connectToPeer(peer);
    }

    public void onClickReset(View view) {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE))
                    .clearApplicationUserData();
        } else {
            Toast.makeText(getApplicationContext(), "Requires at least API 19 (KitKat)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVariables();
        init();
        setPeerDetails();
    }

    private void setPeerDetails() {
        peerAppToApp = (PeerAppToApp) getIntent().getSerializableExtra("PeerAppToApp");
        if(peerAppToApp != null) {
            String address = peerAppToApp.getExternalAddress().toString().substring(1);
            int port = peerAppToApp.getPort();
            editTextDestinationIP.setText(address);
            editTextDestinationPort.setText(port + "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.chain_menu:
                System.out.println("HEAR ME");;
                Log.i(TAG, "Attempting to open chain explorer");
                Intent chainExplorerActivity = new Intent(this, ChainExplorerActivity.class);
                startActivity(chainExplorerActivity);
                return true;
            case R.id.key_recovery_menu:
                Log.i(TAG, "Attempting to open key recovery manager");
                Intent keyShareIntent = new Intent(this, KeyShareActivity.class);
                PrivateKey privateKey = Key.loadKeys(getApplicationContext()).getPrivate();
                keyShareIntent.putExtra("private_key", privateKey);
                startActivity(keyShareIntent);
                return true;
            default:
                Log.i(TAG, "No menu item selected");
                return true;
        }
    }

    private void initVariables() {
        thisActivity = this;
        localIPText = findViewById(R.id.my_local_ip);
        externalIPText = findViewById(R.id.my_external_ip);
        statusText = findViewById(R.id.status);
        statusText.setMovementMethod(new ScrollingMovementMethod());
        editTextDestinationIP = findViewById(R.id.destination_IP);
        editTextDestinationPort = findViewById(R.id.destination_port);
        connectionButton = findViewById(R.id.connection_button);
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(thisActivity);
        //load keys
        KeyPair kp = Key.loadKeys(getApplicationContext());
        communication = new NetworkCommunication(dbHelper, kp, this);

        updateIP();
        updateLocalIPField(getLocalIPAddress());

        //start listening for messages
        communication.start();

    }

    /**
     * Updates the external IP address textfield to the given IP address.
     */
    public void updateExternalIPField(String ipAddress) {
        externalIPText.setText(ipAddress);
        Log.i(TAG, "Updated external IP Address: " + ipAddress);
    }

    /**
     * Updates the internal IP address textfield to the given IP address.
     */
    public void updateLocalIPField(String ipAddress) {
        localIPText.setText(ipAddress);
        Log.i(TAG, "Updated local IP Address:" + ipAddress);
    }

    /**
     * Finds the external IP address of this device by making an API call to https://www.ipify.org/.
     * The networking runs on a separate thread.
     */
    public void updateIP() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
                    final String ip = s.next();
                    // new thread to handle UI updates
                    TrustChainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateExternalIPField(ip);
                        }
                    });
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Finds the local IP address of this device, loops trough network interfaces in order to find it.
     * The address that is not a loopback address is the IP of the device.
     * @return a string representation of the device's IP address
     */
    public String getLocalIPAddress() {
        try {
            List<NetworkInterface> netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface netInt : netInterfaces) {
                List<InetAddress> addresses = Collections.list(netInt.getInetAddresses());
                for (InetAddress addr : addresses) {
                    if(addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void updateLog(final String msg) {
        //just to be sure run it on the ui thread
        //this is not necessary when this function is called from a AsyncTask
        runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      TextView statusText = findViewById(R.id.status);
                      statusText.append(msg);
                  }
              });
    }
}
