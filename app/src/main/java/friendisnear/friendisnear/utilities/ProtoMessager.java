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
import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages.PBLocation;
import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages.PBRequest;
import friendisnear.friendisnear.utilities.mymqttmessages.ProtobufMessages.PBText;

public class ProtoMessager implements CommonActionLitener {
	//private final static Logger LOG = LoggerFactory.getLogger(ProtoMessager.class);
	public final static int REQUEST_FRIEND = 1;
	public final static int REQUEST_ACCEPT_FRIEND = 2;
	public final static int REQUEST_DECLINE_FRIEND = 3;
	public final static int REQUEST_APPOINTMENT = 4;
	public final static int REQUEST_ACCEPT_APPOINTMENT = 5;
	public final static int REQUEST_DECLINE_APPOINTMENT = 6;


	private final int qos = 0;
	
	private boolean connected;
	//private String sendTopic;
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
		//if(user != null && user.getName() != null) sendTopic = TOPIC_PREFIX + user.getName();

		setup();
	}

	private void setup() {

        callback = new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //LOG.info("message arrived Topic: {} ", topic);
				System.out.println("Message arrived from " + topic);
                processProtobufMessage(topic, message.getPayload());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //LOG.trace("deliveryComplete with token {}", token);
				System.out.println("DeliveryComplete with token " + token.getTopics());
            }

            @Override
            public void connectionLost(Throwable cause) {
                //LOG.error("connection lost", cause);
				System.out.println("Connection lost " + cause.toString());
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
						for (Friend fTopic : commons.getFriends().values()) {
								sampleClient.subscribe(fTopic.getTopic(), qos);
						}

						// eigener Channel für Requests und Messages
						sampleClient.subscribe(user.getTopicRequest(), qos);

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
					PBLocation l = pbm.getLocation();
					location.setLatitude(l.getLatidude());
					location.setLongitude(l.getLongitude());
					location.setSpeed(l.getSpeed());
					location.setTime(l.getTimestamp());

					System.out.println("Location Processes: " + location.toString());

					commons.updateLocation(topic, location);
					//LOG.info("got number {} : payload size: {}", number.getNumber(), data.length);
					break;
				case REQUEST:
					if(!topic.equals(user.getTopicRequest())) {
						System.out.println("Requests are only allowed on request topics");
						return;
					}
					PBRequest r = pbm.getRequest();
					System.out.println("Request " + r.getRequest() + " from " + r.getSender() + " received.");
					commons.requestReceived(r.getTimestamp(), r.getSender(), r.getRequest());
					break;
				case TEXT:
					if(!topic.equals(user.getTopicRequest())) {
						System.out.println("Messages are only allowed on request topics");
						return;
					}
					PBText text = pbm.getText();
					System.out.println("Message from " + text.getSender() + ": " + text.getText());
					commons.messageReceived(text.getTimestamp(), text.getSender(), text.getText());
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
		if (connected && user != null && location != null) {
			//LOG.debug("Publishing message: {}", number);
			MqttMessage message = locationMessage(location);
			message.setQos(qos);
			sampleClient.publish(user.getTopic(), message);
			System.out.println("Publishing message: " + message.toString());
		} else {
			//LOG.error("connect first before sending");
		}
	}

	private MqttMessage locationMessage(Location location) {
		// General Message
		ProtobufMessages.PBMessage.Builder builder = ProtobufMessages.PBMessage.newBuilder();
		builder.setSource(clientId.hashCode());
		// Concrete Message
		ProtobufMessages.PBLocation.Builder locationMessage = ProtobufMessages.PBLocation.newBuilder();
		locationMessage.setTimestamp(location.getTime());
		locationMessage.setLatidude(location.getLatitude());
        locationMessage.setLongitude(location.getLongitude());
		locationMessage.setSpeed(location.getSpeed());
		locationMessage.setAvaliable(true);
		builder.setLocation(locationMessage);
		MqttMessage message = new MqttMessage(builder.build().toByteArray());
		return message;
	}

	public void sendRequest(int request, Friend f) {
		if(connected) {
			MqttMessage message = requestMessage(request, f);
			message.setQos(qos);
			try {
				sampleClient.publish(f.getTopicRequest(), message);
			} catch (MqttException e) {
				e.printStackTrace();
			}
			System.out.println("Publishing message: " + message.toString());
		}

	}

	private MqttMessage requestMessage(int request, Friend f) {
		ProtobufMessages.PBMessage.Builder builder = ProtobufMessages.PBMessage.newBuilder();
		builder.setSource(clientId.hashCode());

		ProtobufMessages.PBRequest.Builder requestMessage = ProtobufMessages.PBRequest.newBuilder();
		requestMessage.setTimestamp(System.currentTimeMillis());
		requestMessage.setRequest(request);
		requestMessage.setSender(user.getName());

		builder.setRequest(requestMessage);
		MqttMessage message = new MqttMessage(builder.build().toByteArray());
		return message;
	}
	
	public void sendText(String number, Friend target)  {
		if (connected) {
			//LOG.debug("Publishing message: {}", number);
			MqttMessage message = textMessage(number);
			message.setQos(qos);
			try {
				sampleClient.publish(target.getTopicRequest(), message);
			} catch (MqttException e) {
				e.printStackTrace();
			}
		} else {
			//LOG.error("connect first before sending");
		}
	}
	
	private MqttMessage textMessage(String number) {
		// General Message
		ProtobufMessages.PBMessage.Builder builder = ProtobufMessages.PBMessage.newBuilder();
		builder.setSource(clientId.hashCode());
		// Concrete Message
		ProtobufMessages.PBText.Builder textMessage = ProtobufMessages.PBText.newBuilder();
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
                    sampleClient.subscribe(f.getTopic(), qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case FRIEND_REMOVED:
                try {
                    sampleClient.unsubscribe(f.getTopic());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case USERNAME_CHANGED:
                try {
					sampleClient.unsubscribe(user.getTopicRequest());
					user = commons.getUser();
					sampleClient.subscribe(user.getTopicRequest(), qos);
                    sendLocation(f.getLocation());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                commons.updateUserLocation(null);
                break;
			case FRIEND_REQUEST:
				sendRequest(REQUEST_FRIEND, f);
				break;
			case FRIEND_REQUEST_ACCEPT:
				sendRequest(REQUEST_ACCEPT_FRIEND, f);
				break;
			case FRIEND_REQUEST_DECLINE:
				sendRequest(REQUEST_DECLINE_FRIEND, f);
				break;
			case APPOINTMENT_REQUEST:
				sendRequest(REQUEST_APPOINTMENT, f);
				break;
			case APPOINTMENT_REQUEST_ACCEPT:
				sendRequest(REQUEST_ACCEPT_APPOINTMENT, f);
				break;
			case APPOINTMENT_REQUEST_DECLINE:
				sendRequest(REQUEST_DECLINE_APPOINTMENT, f);
				break;
		}
	}
}
