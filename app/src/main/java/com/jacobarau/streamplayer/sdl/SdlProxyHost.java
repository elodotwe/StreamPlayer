package com.jacobarau.streamplayer.sdl;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import com.jacobarau.net.HTTPClient;
import com.jacobarau.shoutcast.DirectoryClient;
import com.jacobarau.shoutcast.Genre;
import com.jacobarau.streamplayer.MainActivity;
import com.jacobarau.streamplayer.R;
import com.jacobarau.streamplayer.StationPreset;
import com.jacobarau.streamplayer.StreamingService;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.exception.SdlExceptionCause;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.SdlProxyConfigurationResources;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommand;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.Alert;
import com.smartdevicelink.proxy.rpc.AlertManeuverResponse;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.ListFiles;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.MenuParams;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SdlMsgVersion;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.UpdateTurnListResponse;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.AudioStreamingState;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.LayoutMode;
import com.smartdevicelink.proxy.rpc.enums.LockScreenStatus;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Single;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by jacob on 12/25/16.
 */

public class SdlProxyHost implements IProxyListenerALM {
    private static final String TAG = "SdlProxyHost";

    private static final String APP_NAME = "Stream Player";
    private static final String APP_ID = "98765432";

    private static final String ICON_FILENAME = "stream_player.png";
    private int iconCorrelationId;



    List<StationPreset> presets = new LinkedList<>();

    StationPreset currentPreset;

    boolean userWantsStreaming = false;

    List<String> remoteFiles;

    // variable used to increment correlation ID for every request sent to SYNC
    public int autoIncCorrId = 0;


    // variable to create and call functions of the SyncProxy
    private SdlProxyALM proxy = null;

    private boolean lockscreenDisplayed = false;

    private boolean firstNonHmiNone = true;

    private List<Integer> pendingInteractions = new LinkedList<>();

    private final int CMDID_PLAY = 1;
    private final int CMDID_PAUSE = 2;
    private final int CMDID_STATION_BASE = 3;

    Context context = null;

    public SdlProxyHost(Context context) {
        this.context = context;
        presets.add(new StationPreset("DI Nightcore", "http://sc8.radioseven.se:8500"));
        presets.add(new StationPreset("DI Vocal Trance", "http://209.73.138.21:80"));
        presets.add(new StationPreset("DI Vocal Lounge", "http://111.223.51.7:8000"));
        currentPreset = presets.get(0);
    }

    public boolean startProxy() {
        if (proxy == null) {
            try {
                BaseTransportConfig xprt = new TCPTransportConfig(12345, "192.168.1.2", true); //.11 is VM, .6 is ALE
                proxy = new SdlProxyALM(this, APP_NAME, true, APP_ID);//, xprt);

                return true;
            } catch (SdlException e) {
                e.printStackTrace();
                // error creating proxy, returned proxy = null
                if (proxy == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public void disposeSyncProxy() {
        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (SdlException e) {
                e.printStackTrace();
            }
            proxy = null;
            //LockScreenManager.clearLockScreen();
        }
        this.firstNonHmiNone = true;

    }

    public boolean reset() {
        this.firstNonHmiNone = true;
        if (proxy != null) {
            try {
                proxy.resetProxy();
                return true;
            } catch (SdlException e1) {
                e1.printStackTrace();
                //something goes wrong, & the proxy returns as null, stop the service.
                // do not want a running service with a null proxy
                if (proxy == null) {
                    return false;
                }
                return true;
            }
        } else {
            return startProxy();
        }
    }

    /**
     *  Add commands for the app on SDL.
     */
//	public void sendCommands(){
//		AddCommand command = new AddCommand();
//		MenuParams params = new MenuParams();
//		params.setMenuName(TEST_COMMAND_NAME);
//		command = new AddCommand();
//		command.setCmdID(TEST_COMMAND_ID);
//		command.setMenuParams(params);
//		command.setVrCommands(Arrays.asList(new String[]{TEST_COMMAND_NAME}));
//		sendRpcRequest(command);
//	}

    /**
     * Sends an RPC Request to the connected head unit. Automatically adds a correlation id.
     *
     * @param request Request to be sent; correlation ID will be replaced.
     */
    private int sendRpcRequest(RPCRequest request) {
        request.setCorrelationID(autoIncCorrId++);
        try {
            proxy.sendRPCRequest(request);
        } catch (SdlException e) {
            e.printStackTrace();
        }

        return request.getCorrelationID();
    }

    /**
     * Sends the app icon through the uploadImage method with correct params
     *
     * @throws SdlException
     */
    private void sendIcon() throws SdlException {
        iconCorrelationId = autoIncCorrId++;
        uploadImage(R.drawable.ic_launcher, ICON_FILENAME, iconCorrelationId, true);
    }

    /**
     * This method will help upload an image to the head unit
     *
     * @param resource      the R.drawable.__ value of the image you wish to send
     * @param imageName     the filename that will be used to reference this image
     * @param correlationId the correlation id to be used with this request. Helpful for monitoring putfileresponses
     * @param isPersistent  tell the system if the file should stay or be cleared out after connection.
     */
    private void uploadImage(int resource, String imageName, int correlationId, boolean isPersistent) {
        PutFile putFile = new PutFile();
        putFile.setFileType(FileType.GRAPHIC_PNG);
        putFile.setSdlFileName(imageName);
        putFile.setCorrelationID(correlationId);
        putFile.setPersistentFile(isPersistent);
        putFile.setSystemFile(false);
        putFile.setBulkData(contentsOfResource(resource));

        try {
            proxy.sendRPCRequest(putFile);
        } catch (SdlException e) {
            e.printStackTrace();
        }
    }

    private byte[] contentsOfResource(int resource) {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(resource);
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            final int buffersize = 4096;
            final byte[] buffer = new byte[buffersize];
            int available;
            while ((available = is.read(buffer)) >= 0) {
                os.write(buffer, 0, available);
            }
            return os.toByteArray();
        } catch (IOException e) {
            Log.w("SDL Service", "Can't read icon file", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason) {
        Log.v(TAG, "Proxy is closed! " + info + ", " + e + ", " + reason);
        if (!(e instanceof SdlException)) {
            Log.v(TAG, "reset proxy in onproxy closed");
            reset();
        } else if ((((SdlException) e).getSdlExceptionCause() != SdlExceptionCause.SDL_PROXY_CYCLED)) {
            if (((SdlException) e).getSdlExceptionCause() != SdlExceptionCause.BLUETOOTH_DISABLED) {
                Log.v(TAG, "reset proxy in onproxy closed");
                reset();
            }
        }

        clearLockScreen();

        StreamingService.stopPlaying(context);

        //TODO: Check what rev this crept into. You don't even necessarily want the service to spin down
        //upon all proxy closing errors...doesn't quite make sense.
//        stopSelf();
    }


    private void addCommand(String name, int cmdID) {
        Log.v(TAG, "Requesting to add " + name + " with cmdID " + cmdID);
        AddCommand command;
        MenuParams params = new MenuParams();
        params.setMenuName(name);
        command = new AddCommand();
        command.setCmdID(cmdID);
        command.setMenuParams(params);
        command.setVrCommands(Collections.singletonList(name));
        sendRpcRequest(command);
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        if (notification.getHmiLevel().equals(HMILevel.HMI_FULL)) {
            if (notification.getFirstRun()) {
                userWantsStreaming = true;

                //TODO: There has to be a way to show a play and a pause icon separately. Perhaps
                //as part of a Show request? Unsure. This shows a single button with a
                //play/pause icon combined. It does work, but is ugly.
                SubscribeButton but = new SubscribeButton();
                but.setButtonName(ButtonName.OK);
                sendRpcRequest(but);
            }
            // Other HMI (Show, PerformInteraction, etc.) would go here
        }


        if (!notification.getHmiLevel().equals(HMILevel.HMI_NONE)
                && firstNonHmiNone) {

            //uploadImages();
            firstNonHmiNone = false;
            addCommand("Play", CMDID_PLAY);
            addCommand("Pause", CMDID_PAUSE);
            int idx = 0;
            for (StationPreset p : presets) {
                addCommand(p.name, CMDID_STATION_BASE + idx++);
            }

            // Other app setup (SubMenu, CreateChoiceSet, etc.) would go here
        } else {
            //We have HMI_NONE
            if (notification.getFirstRun()) {
                uploadImages();
                firstNonHmiNone = true;
            }
        }

        if (notification.getAudioStreamingState().equals(AudioStreamingState.NOT_AUDIBLE)) {
            if (userWantsStreaming) {
                StreamingService.stopPlaying(context);
            }
        } else {
            if (userWantsStreaming) {
                if (!StreamingService.isStreaming) {
                    StreamingService.startPlaying(context, currentPreset.url);
                }
            }
        }
        Log.i(TAG, "HMI status is " + notification.getAudioStreamingState() + ", " + notification.getHmiLevel() + ", " + notification.getSystemContext() + ", first run " + notification.getFirstRun());
    }



    /**
     * Will show a sample welcome message on screen as well as speak a sample welcome message
     */
//	private void performWelcomeMessage(){
//		try {
//			//Set the welcome message on screen
//			proxy.show(APP_NAME, WELCOME_SHOW, TextAlignment.CENTERED, autoIncCorrId++);
//
//			//Say the welcome message
//			proxy.speak(WELCOME_SPEAK, autoIncCorrId++);
//
//		} catch (SdlException e) {
//			e.printStackTrace();
//		}
//
//	}

    /**
     * Requests list of images to SDL, and uploads images that are missing.
     */
    private void uploadImages() {
        ListFiles listFiles = new ListFiles();
        this.sendRpcRequest(listFiles);

    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        Log.i(TAG, "onListFilesResponse from SDL ");
        if (response.getSuccess()) {
            remoteFiles = response.getFilenames();
        }

        // Check the mutable set for the AppIcon
        // If not present, upload the image
        if (remoteFiles == null || !remoteFiles.contains(SdlProxyHost.ICON_FILENAME)) {
            try {
                sendIcon();
            } catch (SdlException e) {
                e.printStackTrace();
            }
        } else {
            // If the file is already present, send the SetAppIcon request
            try {
                proxy.setappicon(ICON_FILENAME, autoIncCorrId++);
            } catch (SdlException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPutFileResponse(PutFileResponse response) {
        Log.i(TAG, "onPutFileResponse from SDL");
        if (response.getCorrelationID() == iconCorrelationId) { //If we have successfully uploaded our icon, we want to set it
            try {
                proxy.setappicon(ICON_FILENAME, autoIncCorrId++);
            } catch (SdlException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification) {
        if (!lockscreenDisplayed && notification.getShowLockScreen() == LockScreenStatus.REQUIRED) {
            // Show lock screen
            Intent intent = new Intent(context.getApplicationContext(), LockScreenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
            lockscreenDisplayed = true;
            context.startActivity(intent);
        } else if (lockscreenDisplayed && notification.getShowLockScreen() != LockScreenStatus.REQUIRED) {
            // Clear lock screen
            clearLockScreen();
        }
    }

    private void clearLockScreen() {
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        lockscreenDisplayed = false;
    }

    @Override
    public void onOnCommand(OnCommand notification) {
        Log.i(TAG, "onOnCommand: " + notification.getCmdID());
        Integer id = notification.getCmdID();
        if (id != null) {
            switch (id) {
                case CMDID_PLAY:
                    userWantsStreaming = true;
                    StreamingService.startPlaying(context, currentPreset.url);
                    break;
                case CMDID_PAUSE:
                    userWantsStreaming = false;
                    StreamingService.stopPlaying(context);
                    break;
                default:
                    currentPreset = presets.get(id - CMDID_STATION_BASE);
                    userWantsStreaming = true;
                    StreamingService.startPlaying(context, currentPreset.url);
            }
            //onAddCommandClicked(id);
        }
    }

    /**
     * Callback method that runs when the add command response is received from SDL.
     */
    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        Log.i(TAG, "AddCommand response from SDL: " + response.getResultCode().name() + ", " + response.getInfo());

    }


	/*  Vehicle Data   */


    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        Log.i(TAG, "Permision changed: " + notification);
        /* Uncomment to subscribe to vehicle data
        List<PermissionItem> permissions = notification.getPermissionItem();
		for(PermissionItem permission:permissions){
			if(permission.getRpcName().equalsIgnoreCase(FunctionID.SUBSCRIBE_VEHICLE_DATA.name())){
				if(permission.getHMIPermissions().getAllowed()!=null && permission.getHMIPermissions().getAllowed().size()>0){
					if(!isVehicleDataSubscribed){ //If we haven't already subscribed we will subscribe now
						//TODO: Add the vehicle data items you want to subscribe to
						//proxy.subscribevehicledata(gps, speed, rpm, fuelLevel, fuelLevel_State, instantFuelConsumption, externalTemperature, prndl, tirePressure, odometer, beltStatus, bodyInformation, deviceStatus, driverBraking, correlationID);
						proxy.subscribevehicledata(false, true, rpm, false, false, false, false, false, false, false, false, false, false, false, autoIncCorrId++);
					}
				}
			}
		}
		*/
    }

    @Override
    public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
        if (response.getSuccess()) {
            Log.i(TAG, "Subscribed to vehicle data");
        }
    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        Log.i(TAG, "Vehicle data notification from SDL");
        //TODO Put your vehicle data code here
        //ie, notification.getSpeed().

    }

    /**
     * Rest of the SDL callbacks from the head unit
     */

    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
        //TODO: assumes well-behaved module
        pendingInteractions.remove(response.getCorrelationID());
        Log.d(TAG, "Got response to " + response.getCorrelationID() + ", " + pendingInteractions.size() + " pending interactions left");
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {

    }

    @Override
    public void onResetGlobalPropertiesResponse(
            ResetGlobalPropertiesResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        Log.i(TAG, "onOnButtonPress: " + notification.getButtonName());

        if (notification.getButtonName().equals(ButtonName.OK)) {
            if (StreamingService.isStreaming) {
                userWantsStreaming = false;
                StreamingService.stopPlaying(context);
            } else {
                userWantsStreaming = true;
                StreamingService.startPlaying(context, currentPreset.url);
            }
        }
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        // TODO Auto-generated method stub
    }


    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(
            UnsubscribeVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSliderResponse(SliderResponse response) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onOnHashChange(OnHashChange notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnSystemRequest(OnSystemRequest notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnTouchEvent(OnTouchEvent notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnStreamRPC(OnStreamRPC notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStreamRPCResponse(StreamRPCResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDialNumberResponse(DialNumberResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendLocationResponse(SendLocationResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceEnded(OnServiceEnded serviceEnded) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceNACKed(OnServiceNACKed serviceNACKed) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onShowConstantTbtResponse(ShowConstantTbtResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceDataACK() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
        // Some RPCs (depending on region) cannot be sent when driver distraction is active.
    }

    @Override
    public void onError(String info, Exception e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        // TODO Auto-generated method stub
    }

}
