package friendisnear.friendisnear.utilities;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


import com.google.protobuf.InvalidProtocolBufferException;

import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages;
import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages.PBGenericLocation;
import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages.PBGenericText;

public class ProtoMessager implements CommonActionLitener {
	//private final static Logger LOG = LoggerFactory.getLogger(ProtoMessager.class);

	private final int qos = 0;
	
	private boolean connected;
	private String sendTopic;
	private MqttAsyncClient sampleClient;
	private String broker;
	private String clientId;

	private CommonUtility commons;
	private Friend user;


    private MqttCallback callback;

	public ProtoMessager() {
		this("tcp://iot.soft.uni-linz.ac.at:1883", MqttAsyncClient.generateClientId());
	}

	public ProtoMessager(String broker, String clientId) {
		if (clientId == null || broker == null || broker.isEmpty() || clientId.isEmpty()) {
			throw new RuntimeException(String.format("invalid configuration broker: %s  clientId: %s ",
					broker, clientId));
		}

		this.broker = broker;
		this.clientId = clientId;

		commons = CommonUtility.getInstance();
        commons.addCommonActionListener(this);
		user = commons.getUser();
		if(user != null) sendTopic = user.getName();

		setup();
	}

	private void setup() {

        callback = new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //LOG.info("message arrived Topic: {} ", topic);
                processProtobufMessage(topic, message.getPayload());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //LOG.trace("deliveryComplete with token {}", token);
            }

            @Override
            public void connectionLost(Throwable cause) {
                //LOG.error("connection lost", cause);
            }
        };
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			sampleClient = new MqttAsyncClient(this.broker, this.clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(false);
			//LOG.info("Connecting to broker: " + broker);

			sampleClient.setCallback(callback);

			sampleClient.connect(connOpts, null, new IMqttActionListener() {

				public void onSuccess(IMqttToken asyncActionToken) {
					try {
						//LOG.info("connected to {}", ProtoMessager.this.broker);
						if(commons == null) return;
						for (String topic : commons.getFriends().keySet()) {
								sampleClient.subscribe(topic, qos);
						}

						//sampleClient.subscribe(user.getName(), qos);
					} catch (MqttException e) {
						//LOG.error("error subscribing to topic", e);
					}
					connected = true;
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					//LOG.error("failed to connect", exception);
					connected = false;
				}
			});
		} catch (MqttException me) {
			processEx(me);
		}
	}



	private void processProtobufMessage(String topic, byte[] data) {
		ProtobufMessages.PBMessage pbm;
		try {
			pbm = ProtobufMessages.PBMessage.parseFrom(data);
			switch (pbm.getMsgtypeCase()) {
				case LOCATION:
					Location location = new Location("");
					PBGenericLocation l = pbm.getLocation();
					location.setLatitude(l.getLatidude());
					location.setLongitude(l.getLongitude());
					location.setSpeed(l.getSpeed());
					location.setTime(l.getTime());

					commons.updateLocation(topic, location);
					//LOG.info("got number {} : payload size: {}", number.getNumber(), data.length);
					break;
			case TEXT:
					PBGenericText text = pbm.getText();
					//LOG.info("got text {} : payload size: {}", text.getText(), data.length);
					break;
			default:
					//LOG.error("invalid msgtype {}  found", pbm.getMsgtypeCase().name());
					break;
			}
		} catch (InvalidProtocolBufferException e) {
			//LOG.error("message content is invalid - not a valid protobuf message?", e);
		}
	}

	private static void processEx(MqttException me) {
		//LOG.error("exception raised (1): reason: {}; msg; {}; ", me.getReasonCode(), me.getMessage());
		//LOG.error("exception raised (2): error", me);
	}

	public void sendLocation(Location location) throws MqttException {
		if (connected && sendTopic != null && location != null) {
			//LOG.debug("Publishing message: {}", number);
			MqttMessage message = genericLocationMessage(location);
			message.setQos(qos);
			sampleClient.publish(sendTopic, message);
		} else {
			//LOG.error("connect first before sending");
		}
	}

	private MqttMessage genericLocationMessage(Location location) {
		// General Message
		ProtobufMessages.PBMessage.Builder builder = ProtobufMessages.PBMessage.newBuilder();
		builder.setSource(clientId.hashCode());
		// Concrete Message
		ProtobufMessages.PBGenericLocation.Builder locationMessage = ProtobufMessages.PBGenericLocation.newBuilder();
		locationMessage.setTime(location.getTime());
		locationMessage.setLatidude(location.getLatitude());
        locationMessage.setLongitude(location.getLongitude());
		locationMessage.setSpeed(location.getSpeed());
		locationMessage.setAvaliable(true);
		builder.setLocation(locationMessage);
		MqttMessage message = new MqttMessage(builder.build().toByteArray());
		return message;
	}
	
	public void sendText(String number) throws MqttException {
		if (connected) {
			//LOG.debug("Publishing message: {}", number);
			MqttMessage message = genericTextMessage(number);
			message.setQos(qos);
			sampleClient.publish(sendTopic, message);
		} else {
			//LOG.error("connect first before sending");
		}
	}
	
	private MqttMessage genericTextMessage(String number) {
		// General Message
		ProtobufMessages.PBMessage.Builder builder = ProtobufMessages.PBMessage.newBuilder();
		builder.setSource(clientId.hashCode());
		// Concrete Message
		ProtobufMessages.PBGenericText.Builder textMessage = ProtobufMessages.PBGenericText.newBuilder();
		textMessage.setText(number);
		builder.setText(textMessage);
		MqttMessage message = new MqttMessage(builder.build().toByteArray());
		return message;
	}

	public void close() {
		if (connected) {
			try {
				sampleClient.disconnect();
			} catch (MqttException e) {
				processEx(e);
			}
		} else {
			//LOG.error("not connected - cannot disconnect");
		}
	}
	
	private String getDateFromTimeMillis(long timestamp) {
		SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd_HH:mm:ss.SSS");
		Date date = new Date(timestamp);
		return format.format(date);
	}

	@Override
	public void onCommonAction(Friend f, CommonUtility.CommonAction action) {
		switch (action) {
            case FRIEND_ADDED:
                try {
                    sampleClient.subscribe(f.getName(), qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case FRIEND_REMOVED:
                try {
                    sampleClient.unsubscribe(f.getName());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case USERNAME_CHANGED:
                try {
                    sendTopic = commons.getUser().getName();
                    sendLocation(f.getLocation());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                commons.updateUserLocation(null);
                break;
		}
	}
}
