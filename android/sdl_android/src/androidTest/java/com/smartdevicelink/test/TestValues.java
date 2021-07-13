package com.smartdevicelink.test;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.smartdevicelink.R;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.LifecycleManager;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.screen.choiceset.ChoiceCell;
import com.smartdevicelink.managers.screen.menu.MenuCell;
import com.smartdevicelink.managers.screen.menu.MenuConfiguration;
import com.smartdevicelink.managers.screen.menu.MenuSelectionListener;
import com.smartdevicelink.managers.screen.menu.VoiceCommand;
import com.smartdevicelink.managers.screen.menu.VoiceCommandSelectionListener;
import com.smartdevicelink.protocol.SdlProtocol;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.rpc.AppCapability;
import com.smartdevicelink.proxy.rpc.*;
import com.smartdevicelink.proxy.rpc.enums.*;
import com.smartdevicelink.util.Version;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class TestValues {

    //Versions
    public static final Version MAX_RPC_VERSION_SUPPORTED = LifecycleManager.MAX_SUPPORTED_RPC_VERSION;
    /**
     * @see SdlProtocol
     */
    public static final Version MAX_PROTOCOL_VERSION_SUPPORTED = new Version(5, 2, 0);


    // Test Failure Messages
    public static final String NULL = "Value should be null.";
    public static final String MATCH = "Values should match.";
    public static final String ARRAY = "Array values should match.";
    public static final String TRUE = "Value should be true.";
    public static final String FALSE = "Value should be false.";
    public static final String NOT_NULL = "Value should not be null.";
    public static final String JSON_FAIL = "Json testing failed.";

    // RPC Request/Response/Notification/Datatype Test Values
    public static final int GENERAL_INT = 100;
    public static final Integer GENERAL_INTEGER = 100;
    public static final Long GENERAL_LONG = 100L;
    public static final Turn GENERAL_TURN = new Turn();
    public static final Float GENERAL_FLOAT = 100f;
    public static final Image GENERAL_IMAGE = new Image();
    public static final Choice GENERAL_CHOICE = new Choice();
    public static final String GENERAL_STRING = "test";
    public static final Double GENERAL_DOUBLE = 10.01;
    public static final boolean GENERAL_BOOLEAN = true;
    public static final byte[] GENERAL_BYTE_ARRAY = new byte[0];
    public static final TPMS GENERAL_TPMS = TPMS.UNKNOWN;
    public static final TBTState GENERAL_TBTSTATE = TBTState.NEXT_TURN_REQUEST;
    public static final FileType GENERAL_FILETYPE = FileType.BINARY;
    public static final Language GENERAL_LANGUAGE = Language.EN_US;
    public static final HMILevel GENERAL_HMILEVEL = HMILevel.HMI_FULL;
    public static final DIDResult GENERAL_DIDRESULT = new DIDResult();
    public static final TextField GENERAL_TEXTFIELD = new TextField();
    public static final OasisAddress GENERAL_OASISADDRESS = new OasisAddress();
    public static final Coordinate GENERAL_COORDINATE = new Coordinate();
    public static final LocationDetails GENERAL_LOCATIONDETAILS = new LocationDetails();
    public static final Dimension GENERAL_DIMENSION = Dimension._2D;
    public static final ImageType GENERAL_IMAGETYPE = ImageType.DYNAMIC;
    public static final AudioType GENERAL_AUDIOTYPE = AudioType.PCM;
    public static final StartTime GENERAL_STARTTIME = new StartTime();
    public static final TouchType GENERAL_TOUCHTYPE = TouchType.BEGIN;
    public static final TouchEvent GENERAL_TOUCHEVENT = new TouchEvent();
    public static final VrHelpItem GENERAL_VRHELPITEM = new VrHelpItem();
    public static final ImageField GENERAL_IMAGEFIELD = new ImageField();
    public static final DeviceInfo GENERAL_DEVICEINFO = new DeviceInfo();
    public static final AppInfo GENERAL_APPINFO = new AppInfo();
    public static final Uri GENERAL_URI = Uri.parse("http://www.google.com");
    public static final LayoutMode GENERAL_LAYOUTMODE = LayoutMode.LIST_ONLY;
    public static final MenuParams GENERAL_MENUPARAMS = new MenuParams();
    public static final SoftButton GENERAL_SOFTBUTTON = new SoftButton();
    public static final ButtonName GENERAL_BUTTONNAME = ButtonName.OK;
    public static final UpdateMode GENERAL_UPDATEMODE = UpdateMode.RESUME;
    public static final TouchCoord GENERAL_TOUCHCOORD = new TouchCoord();
    public static final MassageModeData GENERAL_MASSAGEMODEDATA = new MassageModeData();
    public static final MassageCushionFirmness GENERAL_MASSAGECUSHIONFIRMNESS = new MassageCushionFirmness();
    public static final DisplayType GENERAL_DISPLAYTYPE = DisplayType.CID;
    public static final VehicleType GENERAL_VEHICLETYPE = new VehicleType();
    public static final RequestType GENERAL_REQUESTTYPE = RequestType.AUTH_REQUEST;
    public static final SystemAction GENERAL_SYSTEMACTION = SystemAction.DEFAULT_ACTION;
    public static final CharacterSet GENERAL_CHARACTERSET = CharacterSet.CID1SET;
    public static final SamplingRate GENERAL_SAMPLINGRATE = SamplingRate._8KHZ;
    public static final ScreenParams GENERAL_SCREENPARAMS = new ScreenParams();
    public static final KeypressMode GENERAL_KEYPRESSMODE = KeypressMode.QUEUE_KEYPRESSES;
    public static final SystemContext GENERAL_SYSTEMCONTEXT = SystemContext.SYSCTXT_MAIN;
    public static final KeyboardEvent GENERAL_KEYBOARDEVENT = KeyboardEvent.ENTRY_SUBMITTED;
    public static final CarModeStatus GENERAL_CARMODESTATUS = CarModeStatus.NORMAL;
    public static final TextFieldName GENERAL_TEXTFIELDNAME = TextFieldName.ETA;
    public static final TriggerSource GENERAL_TRIGGERSOURCE = TriggerSource.TS_VR;
    public static final BitsPerSample GENERAL_BITSPERSAMPLE = BitsPerSample._8_BIT;
    public static final TextAlignment GENERAL_TEXTALIGNMENT = TextAlignment.CENTERED;
    public static final SdlMsgVersion GENERAL_SDLMSGVERSION = new SdlMsgVersion();
    public static final PermissionItem GENERAL_PERMISSIONITEM = new PermissionItem();
    public static final SoftButtonType GENERAL_SOFTBUTTONTYPE = SoftButtonType.SBT_BOTH;
    public static final MassageZone GENERAL_MASSAGEZONE = MassageZone.LUMBAR;
    public static final MassageMode GENERAL_MASSAGEMODE = MassageMode.HIGH;
    public static final WindowStatus GENERAL_WINDOW_STATUS = new WindowStatus();
    public static final MassageCushion GENERAL_MASSAGECUSHION = MassageCushion.BACK_BOLSTERS;
    public static final SeatMemoryActionType GENERAL_SEATMEMORYACTIONTYPE = SeatMemoryActionType.SAVE;
    public static final SupportedSeat GENERAL_SUPPORTEDSEAT = SupportedSeat.DRIVER;
    public static final KeyboardLayout GENERAL_KEYBOARDLAYOUT = KeyboardLayout.QWERTY;
    public static final ImageFieldName GENERAL_IMAGEFIELDNAME = ImageFieldName.graphic;
    public static final HMIPermissions GENERAL_HMIPERMISSIONS = new HMIPermissions();
    public static final IgnitionStatus GENERAL_IGNITIONSTATUS = IgnitionStatus.RUN;
    public static final ButtonEventMode GENERAL_BUTTONEVENTMODE = ButtonEventMode.BUTTONUP;
    public static final ButtonPressMode GENERAL_BUTTONPRESSMODE = ButtonPressMode.SHORT;
    public static final PowerModeStatus GENERAL_POWERMODESTATUS = PowerModeStatus.RUNNING_2;
    public static final VehicleDataType GENERAL_VEHICLEDATATYPE = VehicleDataType.VEHICLEDATA_BRAKING;
    public static final InteractionMode GENERAL_INTERACTIONMODE = InteractionMode.BOTH;
    public static final ImageResolution GENERAL_IMAGERESOLUTION = new ImageResolution();
    public static final FuelCutoffStatus GENERAL_FUELCUTOFFSTATUS = FuelCutoffStatus.NORMAL_OPERATION;
    public static final CompassDirection GENERAL_COMPASSDIRECTION = CompassDirection.EAST;
    public static final LockScreenStatus GENERAL_LOCKSCREENSTATUS = LockScreenStatus.REQUIRED;
    public static final VehicleDataStatus GENERAL_VEHICLEDATASTATUS = VehicleDataStatus.ON;
    public static final DeviceLevelStatus GENERAL_DEVICELEVELSTATUS = DeviceLevelStatus.FOUR_LEVEL_BARS;
    public static final ButtonCapabilities GENERAL_BUTTONCAPABILITIES = new ButtonCapabilities();
    public static final EmergencyEventType GENERAL_EMERGENCYEVENTTYPE = EmergencyEventType.FAULT;
    public static final AmbientLightStatus GENERAL_AMBIENTLIGHTSTATUS = AmbientLightStatus.NIGHT;
    public static final SpeechCapabilities GENERAL_SPEECHCAPABILITIES = SpeechCapabilities.TEXT;
    public static final WarningLightStatus GENERAL_WARNINGLIGHTSTATUS = WarningLightStatus.OFF;
    public static final KeyboardProperties GENERAL_KEYBOARDPROPERTIES = new KeyboardProperties();
    public static final PrimaryAudioSource GENERAL_PRIMARYAUDIOSOURCE = PrimaryAudioSource.BLUETOOTH_STEREO_BTST;
    public static final AudioStreamingState GENERAL_AUDIOSTREAMINGSTATE = AudioStreamingState.AUDIBLE;
    public static final VideoStreamingState GENERAL_VIDEOSTREAMINGSTATE = VideoStreamingState.STREAMABLE;
    public static final DisplayCapabilities GENERAL_DISPLAYCAPABILITIES = new DisplayCapabilities();
    public static final ParameterPermissions GENERAL_PARAMETERPERMISSIONS = new ParameterPermissions();
    public static final IgnitionStableStatus GENERAL_IGNITIONSTABLESTATUS = IgnitionStableStatus.IGNITION_SWITCH_STABLE;
    public static final VehicleDataResultCode GENERAL_VEHICLEDATARESULTCODE = VehicleDataResultCode.IGNORED;
    public static final ComponentVolumeStatus GENERAL_COMPONENTVOLUMESTATUS = ComponentVolumeStatus.LOW;
    public static final PresetBankCapabilities GENERAL_PRESETBANKCAPABILITIES = new PresetBankCapabilities();
    public static final CapacityUnit GENERAL_CAPACITYUNIT = CapacityUnit.KILOGRAMS;
    public static final VehicleDataEventStatus GENERAL_VEHCILEDATAEVENTSTATUS = VehicleDataEventStatus.YES;
    public static final VehicleDataEventStatus GENERAL_VEHICLEDATAEVENTSTATUS = VehicleDataEventStatus.YES;
    public static final TouchEventCapabilities GENERAL_TOUCHEVENTCAPABILITIES = new TouchEventCapabilities();
    public static final SeatMemoryAction GENERAL_SEATMEMORYACTION = new SeatMemoryAction();
    public static final SoftButtonCapabilities GENERAL_SOFTBUTTONCAPABILITIES = new SoftButtonCapabilities();
    public static final ECallConfirmationStatus GENERAL_ECALLCONFIRMATIONSTATUS = ECallConfirmationStatus.CALL_IN_PROGRESS;
    public static final AudioPassThruCapabilities GENERAL_AUDIOPASSTHRUCAPABILITIES = new AudioPassThruCapabilities();
    public static final PowerModeQualificationStatus GENERAL_POWERMODEQUALIFICATIONSTATUS = PowerModeQualificationStatus.POWER_MODE_OK;
    public static final VehicleDataNotificationStatus GENERAL_VEHICLEDATANOTIFICATIONSTATUS = VehicleDataNotificationStatus.NORMAL;
    public static final AppInterfaceUnregisteredReason GENERAL_APPINTERFACEUNREGISTEREDREASON = AppInterfaceUnregisteredReason.BLUETOOTH_OFF;
    public static final SystemCapabilityType GENERAL_SYSTEMCAPABILITYTYPE = SystemCapabilityType.NAVIGATION;
    public static final NavigationCapability GENERAL_NAVIGATIONCAPABILITY = new NavigationCapability();
    public static final DriverDistractionCapability GENERAL_DRIVERDISTRACTIONCAPABILITY = new DriverDistractionCapability();
    public static final PhoneCapability GENERAL_PHONECAPABILITY = new PhoneCapability();
    public static final RemoteControlCapabilities GENERAL_REMOTECONTROLCAPABILITIES = new RemoteControlCapabilities();
    public static final SystemCapability GENERAL_SYSTEMCAPABILITY = new SystemCapability();
    public static final VideoStreamingProtocol GENERAL_VIDEOSTREAMINGPROTOCOL = VideoStreamingProtocol.RAW;
    public static final VideoStreamingCodec GENERAL_VIDEOSTREAMINGCODEC = VideoStreamingCodec.H264;
    public static final VideoStreamingCapability GENERAL_VIDEOSTREAMINGCAPABILITY = new VideoStreamingCapability();
    public static final VideoStreamingCapability GENERAL_ADDITIONAL_CAPABILITY = new VideoStreamingCapability();
    public static final List<VideoStreamingCapability> GENERAL_ADDITIONAL_CAPABILITY_LIST = new ArrayList<>();
    public static final VideoStreamingFormat GENERAL_VIDEOSTREAMINGFORMAT = new VideoStreamingFormat();
    public static final RGBColor GENERAL_RGBCOLOR = new RGBColor();
    public static final TemplateColorScheme GENERAL_DAYCOLORSCHEME = new TemplateColorScheme();
    public static final TemplateColorScheme GENERAL_NIGHTCOLORSCHEME = new TemplateColorScheme();
    public static final Result GENERAL_RESULT = Result.SUCCESS;
    public static final WayPointType GENERAL_WAYPOINTTYPE = WayPointType.DESTINATION;
    public static final SingleTireStatus GENERAL_SINGLETIRESTATUS = new SingleTireStatus();
    public static final DriverDistractionState GENERAL_DRIVERDISTRACTIONSTATE = DriverDistractionState.DD_ON;
    public static final List<LocationDetails> GENERAL_LOCATIONDETAILS_LIST = Arrays.asList(new LocationDetails[]{TestValues.GENERAL_LOCATIONDETAILS, TestValues.GENERAL_LOCATIONDETAILS});
    public static final AudioStreamingIndicator GENERAL_AUDIO_STREAMING_INDICATOR = AudioStreamingIndicator.PLAY;
    public static final SeekStreamingIndicator GENERAL_SEEK_STREAMING_INDICATOR = new SeekStreamingIndicator(SeekIndicatorType.TRACK);
    public static final SeekIndicatorType GENERAL_SEEKINDICATORTYPE = SeekIndicatorType.TRACK;
    public static final String GENERAL_APP_ID = "123e4567e8";
    public static final String GENERAL_FULL_APP_ID = "123e4567-e89b-12d3-a456-426655440000";
    public static final String GENERAL_OEM_CUSTOM_VEHICLE_DATA_NAME = "oemCustomVehicleDataName";
    public static final HybridAppPreference GENERAL_HYBRID_APP_PREFERENCE = HybridAppPreference.CLOUD;
    public static final CloudAppProperties GENERAL_CLOUDAPPPROPERTIES = new CloudAppProperties();
    public static final AppServiceType GENERAL_APP_SERVICE_TYPE = AppServiceType.MEDIA;
    public static final List<Integer> GENERAL_FUNCTION_ID_LIST = Arrays.asList(FunctionID.GET_VEHICLE_DATA.getId(), FunctionID.SEND_HAPTIC_DATA.getId());
    public static final AppServiceManifest GENERAL_APPSERVICEMANIFEST = new AppServiceManifest(AppServiceType.MEDIA.name());
    public static final MediaServiceManifest GENERAL_MEDIA_SERVICE_MANIFEST = new MediaServiceManifest();
    public static final WeatherServiceManifest GENERAL_WEATHER_SERVICE_MANIFEST = new WeatherServiceManifest();
    public static final NavigationServiceManifest GENERAL_NAVIGATION_SERVICE_MANIFEST = new NavigationServiceManifest();
    public static final AppServiceRecord GENERAL_APPSERVICERECORD = new AppServiceRecord();
    public static final AppServiceCapability GENERAL_APP_SERVICE_CAPABILITY = new AppServiceCapability();
    public static final AppServicesCapabilities GENERAL_APP_SERVICE_CAPABILITIES = new AppServicesCapabilities();
    public static final ServiceUpdateReason GENERAL_SERVICE_UPDATE_REASON = ServiceUpdateReason.MANIFEST_UPDATE;
    public static final DateTime GENERAL_DATETIME = new DateTime();
    public static final WeatherData GENERAL_WEATHERDATA = new WeatherData();
    public static final WeatherAlert GENERAL_WEATHERALERT = new WeatherAlert();
    public static final MediaType GENERAL_MEDIATYPE = MediaType.MUSIC;
    public static final MediaServiceData GENERAL_MEDIASERVICEDATA = new MediaServiceData();
    public static final WeatherServiceData GENERAL_WEATHERSERVICEDATA = new WeatherServiceData();
    public static final NavigationServiceData GENERAL_NAVIGATIONSERVICEDATA = new NavigationServiceData();
    public static final AppServiceData GENERAL_APPSERVICEDATA = new AppServiceData();
    public static final NavigationAction GENERAL_NAVIGATIONACTION = NavigationAction.STAY;
    public static final NavigationJunction GENERAL_NAVIGATION_JUNCTION = NavigationJunction.BIFURCATION;
    public static final Direction GENERAL_DIRECTION = Direction.RIGHT;
    public static final NavigationInstruction GENERAL_NAVIGATION_INSTRUCTION = new NavigationInstruction();
    public static final Version GENERAL_VERSION = new Version("4.0.0");
    public static final ModuleType GENERAL_MODULETYPE = ModuleType.CLIMATE;
    public static final Temperature GENERAL_TEMPERATURE = new Temperature();
    public static final TemperatureUnit GENERAL_TEMPERATUREUNIT = TemperatureUnit.CELSIUS;
    public static final DefrostZone GENERAL_DEFROSTZONE = DefrostZone.ALL;
    public static final VentilationMode GENERAL_VENTILATIONMODE = VentilationMode.BOTH;
    public static final LightName GENERAL_LIGHTNAME = LightName.AMBIENT_LIGHTS;
    public static final DisplayMode GENERAL_DISPLAYMODE = DisplayMode.AUTO;
    public static final DistanceUnit GENERAL_DISTANCEUNIT = DistanceUnit.KILOMETERS;
    public static final LightStatus GENERAL_LIGHTSTATUS = LightStatus.OFF;
    public static final RadioBand GENERAL_RADIOBAND = RadioBand.AM;
    public static final AppCapabilityType GENERAL_APPCAPABILITYTYPE = AppCapabilityType.VIDEO_STREAMING;
    public static final AppCapability GENERAL_APPCAPABILITY = new AppCapability();
    public static final ClimateControlData GENERAL_CLIMATECONTROLDATA = new ClimateControlData();
    public static final ClimateData GENERAL_CLIMATEDATA = new ClimateData();
    public static final SeatControlData GENERAL_SEATCONTROLDATA = new SeatControlData();
    public static final RdsData GENERAL_RDSDATA = new RdsData();
    public static final StationIDNumber GENERAL_STATIONIDNUMBER = new StationIDNumber();
    public static final SisData GENERAL_SISDATA = new SisData();
    public static final RadioState GENERAL_RADIOSTATE = RadioState.ACQUIRED;
    public static final RadioControlData GENERAL_RADIOCONTROLDATA = new RadioControlData();
    public static final ModuleData GENERAL_MODULEDATA = new ModuleData();
    public static final ClimateControlCapabilities GENERAL_CLIMATECONTROLCAPABILITIES = new ClimateControlCapabilities();
    public static final RadioControlCapabilities GENERAL_RADIOCONTROLCAPABILITIES = new RadioControlCapabilities();
    public static final SeatControlCapabilities GENERAL_SEATCONTROLCAPABILITIES = new SeatControlCapabilities();
    public static final EqualizerSettings GENERAL_EQUALIZERSETTINGS = new EqualizerSettings();
    public static final LightCapabilities GENERAL_LIGHTCAPABILITIES = new LightCapabilities();
    public static final LightState GENERAL_LIGHTSTATE = new LightState();
    public static final AudioControlCapabilities GENERAL_AUDIOCONTROLCAPABILITIES = new AudioControlCapabilities();
    public static final HMISettingsControlCapabilities GENERAL_HMISETTINGSCONTROLCAPABILITIES = new HMISettingsControlCapabilities();
    public static final LightControlCapabilities GENERAL_LIGHTCONTROLCAPABILITIES = new LightControlCapabilities();
    public static final AudioControlData GENERAL_AUDIOCONTROLDATA = new AudioControlData();
    public static final LightControlData GENERAL_LIGHTCONTROLDATA = new LightControlData();
    public static final HMISettingsControlData GENERAL_HMISETTINGSCONTROLDATA = new HMISettingsControlData();
    public static final DynamicUpdateCapabilities GENERAL_DYNAMICUPDATECAPABILITIES = new DynamicUpdateCapabilities();
    public static final KeyboardCapabilities GENERAL_KEYBOARD_CAPABILITIES = new KeyboardCapabilities();
    public static final WindowState GENERAL_WINDOWSTATE = new WindowState();

    public static final SeatOccupancy GENERAL_SEAT_OCCUPANCY = new SeatOccupancy();
    public static final List<SeatStatus> GENERAL_SEATS_OCCUPIED = new ArrayList<SeatStatus>(1);
    public static final List<SeatStatus> GENERAL_SEATS_BELTED = new ArrayList<SeatStatus>(1);
    public static final SeatStatus GENERAL_SEAT_STATUS = new SeatStatus();
    public static final SeatLocation GENERAL_SEATLOCATION = new SeatLocation();
    public static final DoorStatusType GENERAL_DOORSTATUSTYPE = DoorStatusType.REMOVED;

    public static final DoorStatus GENERAL_DOOR_STATUS = new DoorStatus();
    public static final GateStatus GENERAL_GATE_STATUS = new GateStatus();
    public static final RoofStatus GENERAL_ROOF_STATUS = new RoofStatus();

    public static final ArrayList<DoorStatus> GENERAL_DOOR_STATUS_LIST = new ArrayList<>(1);
    public static final ArrayList<GateStatus> GENERAL_GATE_STATUS_LIST = new ArrayList<>(1);
    public static final ArrayList<RoofStatus> GENERAL_ROOF_STATUS_LIST = new ArrayList<>(1);

    public static final VehicleDataResult GENERAL_OEM_CUSTOM_VEHICLE_DATA = new VehicleDataResult();
    public static final TemplateConfiguration GENERAL_TEMPLATE_CONFIGURATION = new TemplateConfiguration();
    public static final WindowTypeCapabilities GENERAL_WINDOW_TYPE_CAPABILITIES = new WindowTypeCapabilities();
    public static final WindowCapability GENERAL_WINDOW_CAPABILITY = new WindowCapability();
    public static final DisplayCapability GENERAL_DISPLAY_CAPABILITY = new DisplayCapability();

    public static final SdlArtwork GENERAL_ARTWORK = new SdlArtwork("sdl", FileType.GRAPHIC_PNG, R.drawable.ic_sdl, false);
    public static final SdlArtwork GENERAL_ARTWORK_STATIC = new SdlArtwork(StaticIconName.BACK);
    public static final MenuLayout GENERAL_MENU_LAYOUT = MenuLayout.LIST;
    public static final MenuConfiguration GENERAL_MENU_CONFIGURATION = new MenuConfiguration(GENERAL_MENU_LAYOUT, GENERAL_MENU_LAYOUT);

    public static final HMICapabilities GENERAL_HMICAPABILITIES = new HMICapabilities();

    public static final MetadataTags GENERAL_METADATASTRUCT = new MetadataTags();
    public static final Rectangle GENERAL_RECTANGLE = new Rectangle();
    public static final HapticRect GENERAL_HAPTIC_RECT = new HapticRect();
    public static final FuelType GENERAL_FUELTYPE = FuelType.GASOLINE;
    public static final LockScreenConfig GENERAL_LOCKSCREENCONFIG = new LockScreenConfig();
    public static final Grid GENERAL_GRID = new Grid();
    public static final ModuleInfo GENERAL_MODULE_INFO = new ModuleInfo();
    public static final StabilityControlsStatus GENERAL_STABILITY_CONTROL_STATUS = new StabilityControlsStatus();
    public static final VehicleDataStatus GENERAL_ESC_SYSTEM = VehicleDataStatus.ON;
    public static final VehicleDataStatus GENERAL_S_WAY_CONTROL = VehicleDataStatus.OFF;
    public static final WindowType GENERAL_WINDOWTYPE = WindowType.MAIN;
    public static final GearStatus GENERAL_GEAR_STATUS = new GearStatus();
    public static final PRNDL GENERAL_USER_SELECTED_GEAR = PRNDL.NEUTRAL;
    public static final PRNDL GENERAL_ACTUAL_GEAR = PRNDL.DRIVE;
    public static final TransmissionType GENERAL_TRANSMISSION_TYPE = TransmissionType.AUTOMATIC;
    public static final Grid GENERAL_LOCATION_GRID = new Grid(2, 3);
    public static final WindowState GENERAL_WINDOW_STATE = new WindowState(2, 3);
    public static final Integer GENERAL_APPROX_POSITION = new Integer(3);
    public static final Integer GENERAL_DEVIATION = new Integer(2);
    public static final List<Long> GENERAL_LONG_LIST = Arrays.asList(new Long[]{1L, 2L});
    public static final List<Turn> GENERAL_TURN_LIST = new ArrayList<Turn>();
    public static final List<Choice> GENERAL_CHOICE_LIST = new ArrayList<Choice>();
    public static final List<String> GENERAL_STRING_LIST = Arrays.asList(new String[]{"a", "b"});
    public static final KeyboardInputMask GENERAL_KEYBOARD_INPUT_MASK = KeyboardInputMask.ENABLE_INPUT_KEY_MASK;
    public static final KeyboardLayoutCapability GENERAL_KEYBOARD_LAYOUT_CAPABILITY = new KeyboardLayoutCapability(GENERAL_KEYBOARDLAYOUT, 1);
    public static final List<KeyboardLayoutCapability> GENERAL_SUPPORTED_KEYBOARDS_LIST = new ArrayList<KeyboardLayoutCapability>();
    public static final List<Integer> GENERAL_INTEGER_LIST = Arrays.asList(new Integer[]{-1, -2});
    public static final List<TTSChunk> GENERAL_TTSCHUNK_LIST = new ArrayList<TTSChunk>(2);
    public static final List<HMILevel> GENERAL_HMILEVEL_LIST = Arrays.asList(new HMILevel[]{HMILevel.HMI_FULL, HMILevel.HMI_BACKGROUND});
    public static final List<FileType> GENERAL_FILETYPE_LIST = new ArrayList<FileType>(1);
    public static final List<TextField> GENERAL_TEXTFIELD_LIST = new ArrayList<TextField>(1);
    public static final List<DIDResult> GENERAL_DIDRESULT_LIST = new ArrayList<DIDResult>(1);
    public static final List<TouchCoord> GENERAL_TOUCHCOORD_LIST = new ArrayList<TouchCoord>(1);
    public static final List<MassageModeData> GENERAL_MASSAGEMODEDATA_LIST = new ArrayList<MassageModeData>(1);
    public static final List<MassageCushionFirmness> GENERAL_MASSAGECUSHIONFIRMNESS_LIST = new ArrayList<MassageCushionFirmness>(1);
    public static final List<AppHMIType> GENERAL_APPHMITYPE_LIST = new ArrayList<AppHMIType>(2);
    public static final List<VrHelpItem> GENERAL_VRHELPITEM_LIST = new ArrayList<VrHelpItem>(2);
    public static final List<SoftButton> GENERAL_SOFTBUTTON_LIST = new ArrayList<SoftButton>(1);
    public static final List<ImageField> GENERAL_IMAGEFIELD_LIST = new ArrayList<ImageField>(1);
    public static final List<TouchEvent> GENERAL_TOUCHEVENT_LIST = new ArrayList<TouchEvent>(1);
    public static final List<PermissionItem> GENERAL_PERMISSIONITEM_LIST = new ArrayList<PermissionItem>(1);
    public static final List<GlobalProperty> GENERAL_GLOBALPROPERTY_LIST = new ArrayList<GlobalProperty>(2);
    public static final List<VrCapabilities> GENERAL_VRCAPABILITIES_LIST = new ArrayList<VrCapabilities>(1);
    public static final List<MediaClockFormat> GENERAL_MEDIACLOCKFORMAT_LIST = new ArrayList<MediaClockFormat>(2);
    public static final List<VehicleDataResult> GENERAL_VEHICLEDATARESULT_LIST = new ArrayList<VehicleDataResult>(VehicleDataType.values().length);
    public static final List<PrerecordedSpeech> GENERAL_PRERECORDEDSPEECH_LIST = new ArrayList<PrerecordedSpeech>(2);
    public static final List<SpeechCapabilities> GENERAL_SPEECHCAPABILITIES_LIST = new ArrayList<SpeechCapabilities>(2);
    public static final List<ButtonCapabilities> GENERAL_BUTTONCAPABILITIES_LIST = new ArrayList<ButtonCapabilities>(2);
    public static final List<HmiZoneCapabilities> GENERAL_HMIZONECAPABILITIES_LIST = new ArrayList<HmiZoneCapabilities>(2);
    public static final List<SoftButtonCapabilities> GENERAL_SOFTBUTTONCAPABILITIES_LIST = new ArrayList<SoftButtonCapabilities>(1);
    public static final List<AudioPassThruCapabilities> GENERAL_AUDIOPASSTHRUCAPABILITIES_LIST = new ArrayList<AudioPassThruCapabilities>(1);
    public static final List<VideoStreamingFormat> GENERAL_VIDEOSTREAMINGFORMAT_LIST = new ArrayList<VideoStreamingFormat>(2);
    public static final List<DefrostZone> GENERAL_DEFROSTZONE_LIST = Arrays.asList(new DefrostZone[]{DefrostZone.FRONT, DefrostZone.REAR});
    public static final List<VentilationMode> GENERAL_VENTILATIONMODE_LIST = Arrays.asList(new VentilationMode[]{VentilationMode.LOWER, VentilationMode.UPPER});
    public static final List<ClimateControlCapabilities> GENERAL_CLIMATECONTROLCAPABILITIES_LIST = new ArrayList<ClimateControlCapabilities>(1);
    public static final List<RadioControlCapabilities> GENERAL_RADIOCONTROLCAPABILITIES_LIST = new ArrayList<RadioControlCapabilities>(1);
    public static final Vector<String> GENERAL_VECTOR_STRING = new Vector<>(Arrays.asList(new String[]{"a", "b"}));
    public static final Vector<TTSChunk> GENERAL_VECTOR_TTS_CHUNKS = new Vector<>(Arrays.asList(new TTSChunk("Welcome to the jungle", SpeechCapabilities.TEXT)));
    public static final List<SeatControlCapabilities> GENERAL_SEATCONTROLCAPABILITIES_LIST = new ArrayList<SeatControlCapabilities>(1);
    public static final List<EqualizerSettings> GENERAL_EQUALIZERSETTINGS_LIST = new ArrayList<EqualizerSettings>(1);
    public static final List<LightCapabilities> GENERAL_LIGHTCAPABILITIES_LIST = new ArrayList<LightCapabilities>(1);
    public static final List<LightState> GENERAL_LIGHTSTATE_LIST = new ArrayList<LightState>(1);
    public static final List<AudioControlCapabilities> GENERAL_AUDIOCONTROLCAPABILITIES_LIST = new ArrayList<AudioControlCapabilities>(1);
    public static final List<ModuleData> GENERAL_MODULEDATA_LIST = Collections.singletonList(GENERAL_MODULEDATA);
    public static final List<AppServiceType> GENERAL_APPSERVICETYPE_LIST = Arrays.asList(AppServiceType.MEDIA, AppServiceType.NAVIGATION);
    public static final List<AppServiceCapability> GENERAL_APPSERVICECAPABILITY_LIST = Arrays.asList(GENERAL_APP_SERVICE_CAPABILITY);
    public static final List<WeatherData> GENERAL_WEATHERDATA_LIST = Arrays.asList(GENERAL_WEATHERDATA);
    public static final List<WeatherAlert> GENERAL_WEATHERALERT_LIST = Arrays.asList(GENERAL_WEATHERALERT);
    public static final List<NavigationInstruction> GENERAL_NAVIGATION_INSTRUCTION_LIST = Arrays.asList(GENERAL_NAVIGATION_INSTRUCTION);
    public static final List<SeatLocation> GENERAL_SEAT_LIST = new ArrayList<>(1);
    public static final List<Boolean> GENERAL_BOOLEAN_LIST = Arrays.asList(new Boolean[]{Boolean.TRUE, Boolean.TRUE});
    public static final List<Integer> GENERAL_AVAILABLE_HD_CHANNELS_LIST = Arrays.asList(new Integer[]{1, 2});
    public static final List<ImageType> GENERAL_IMAGE_TYPE_LIST = Arrays.asList(new ImageType[]{ImageType.DYNAMIC, ImageType.STATIC});
    public static final List<WindowTypeCapabilities> GENERAL_WINDOW_TYPE_CAPABILITIES_LIST = new ArrayList<WindowTypeCapabilities>(1);
    public static final List<WindowCapability> GENERAL_WINDOW_CAPABILITY_LIST = new ArrayList<WindowCapability>(1);
    public static final List<DisplayCapability> GENERAL_DISPLAYCAPABILITY_LIST = new ArrayList<DisplayCapability>(1);
    public static final List<MenuLayout> GENERAL_MENU_LAYOUT_LIST = Arrays.asList(MenuLayout.LIST, MenuLayout.TILES);
    public static final int GENERAL_MENU_MAX_ID = 2000000000;
    public static final MenuCell GENERAL_MENUCELL = new MenuCell(GENERAL_STRING, null, null, new MenuSelectionListener() {
        @Override
        public void onTriggered(TriggerSource trigger) {
            //
        }
    });
    public static final ChoiceCell GENERAL_CHOICECELL = new ChoiceCell(GENERAL_STRING);
    public static final List<ChoiceCell> GENERAL_CHOICECELL_LIST = Arrays.asList(GENERAL_CHOICECELL);
    public static final List<MenuCell> GENERAL_MENUCELL_LIST = Arrays.asList(GENERAL_MENUCELL);
    public static final VoiceCommand GENERAL_VOICE_COMMAND = new VoiceCommand(GENERAL_STRING_LIST, new VoiceCommandSelectionListener() {
        @Override
        public void onVoiceCommandSelected() {

        }
    });
    public static final List<VoiceCommand> GENERAL_VOICE_COMMAND_LIST = Arrays.asList(GENERAL_VOICE_COMMAND);

    public static final JSONArray JSON_TURNS = new JSONArray();
    public static final JSONArray JSON_CHOICES = new JSONArray();
    public static final JSONArray JSON_HMILEVELS = new JSONArray();
    public static final JSONArray JSON_TTSCHUNKS = new JSONArray();
    public static final JSONArray JSON_DIDRESULTS = new JSONArray();
    public static final JSONArray JSON_TEXTFIELDS = new JSONArray();
    public static final JSONArray JSON_TOUCHCOORDS = new JSONArray();
    public static final JSONArray JSON_VRHELPITEMS = new JSONArray();
    public static final JSONArray JSON_SOFTBUTTONS = new JSONArray();
    public static final JSONArray JSON_IMAGEFIELDS = new JSONArray();
    public static final JSONArray JSON_TOUCHEVENTS = new JSONArray();
    public static final JSONArray JSON_PERMISSIONITEMS = new JSONArray();
    public static final JSONArray JSON_BUTTONCAPABILITIES = new JSONArray();
    public static final JSONArray JSON_SOFTBUTTONCAPABILITIES = new JSONArray();
    public static final JSONArray JSON_AUDIOPASSTHRUCAPABILITIES = new JSONArray();
    public static final JSONArray JSON_RADIOCONTROLCAPABILITIES = new JSONArray();
    public static final JSONArray JSON_CLIMATECONTROLCAPABILITIES = new JSONArray();
    public static final JSONArray JSON_TEXTFIELDTYPES = new JSONArray();
    public static final JSONArray JSON_SEAT_LOCATIONS = new JSONArray();
    public static final JSONObject JSON_TURN = new JSONObject();
    public static final JSONObject JSON_IMAGE = new JSONObject();
    public static final JSONObject JSON_CHOICE = new JSONObject();
    public static final JSONObject JSON_DIDRESULT = new JSONObject();
    public static final JSONObject JSON_STARTTIME = new JSONObject();
    public static final JSONObject JSON_TEXTFIELD = new JSONObject();
    public static final JSONObject JSON_TOUCHCOORD = new JSONObject();
    public static final JSONObject JSON_TOUCHEVENT = new JSONObject();
    public static final JSONObject JSON_IMAGEFIELD = new JSONObject();
    public static final JSONObject JSON_SOFTBUTTON = new JSONObject();
    public static final JSONObject JSON_MENUPARAMS = new JSONObject();
    public static final JSONObject JSON_DEVICEINFO = new JSONObject();
    public static final JSONObject JSON_APPINFO = new JSONObject();
    public static final JSONObject JSON_VRHELPITEM = new JSONObject();
    public static final JSONObject JSON_SCREENPARAMS = new JSONObject();
    public static final JSONObject JSON_SDLMSGVERSION = new JSONObject();
    public static final JSONObject JSON_PERMISSIONITEM = new JSONObject();
    public static final JSONObject JSON_HMIPERMISSIONS = new JSONObject();
    public static final JSONObject JSON_IMAGERESOLUTION = new JSONObject();
    public static final JSONObject JSON_KEYBOARDPROPERTIES = new JSONObject();
    public static final JSONObject JSON_DISPLAYCAPABILITIES = new JSONObject();
    public static final JSONObject JSON_PARAMETERPERMISSIONS = new JSONObject();
    public static final JSONObject JSON_PRESETBANKCAPABILITIES = new JSONObject();
    public static final JSONObject JSON_TOUCHEVENTCAPABILITIES = new JSONObject();
    public static final JSONObject JSON_PCMSTREAMCAPABILITIES = new JSONObject();
    public static final JSONObject JSON_RGBCOLOR = new JSONObject();
    public static final JSONObject JSON_DAYCOLORSCHEME = new JSONObject();
    public static final JSONObject JSON_NIGHTCOLORSCHEME = new JSONObject();
    public static final JSONObject JSON_GRID = new JSONObject();
    public static final JSONObject JSON_MODULE_INFO = new JSONObject();
    public static final JSONArray JSON_IMAGE_TYPE_SUPPORTED = new JSONArray();
    public static final JSONObject JSON_WINDOW_TYPE_CAPABILITIES = new JSONObject();
    public static final JSONArray JSON_WINDOW_TYPE_CAPABILITIES_LIST = new JSONArray();
    public static final JSONObject JSON_WINDOW_CAPABILITY = new JSONObject();
    public static final JSONArray JSON_WINDOW_CAPABILITIES = new JSONArray();
    public static final JSONArray JSON_IMAGE_TYPES = new JSONArray();
    public static final JSONObject JSON_DISPLAYCAPABILITY = new JSONObject();
    public static final JSONArray JSON_DISPLAYCAPABILITY_LIST = new JSONArray();
    public static final JSONArray JSON_SUPPORTED_KEYBOARDS_LIST = new JSONArray();
    public static final JSONObject JSON_DYNAMICUPDATECAPABILITIES = new JSONObject();
    public static final JSONObject JSON_KEYBOARD_CAPABILITY = new JSONObject();
    public static final JSONArray JSON_ROOF_STATUSES = new JSONArray();
    public static final JSONArray JSON_DOOR_STATUSES = new JSONArray();
    public static final JSONArray JSON_GATE_STATUSES = new JSONArray();
    public static final JSONObject JSON_ROOF_STATUS = new JSONObject();
    public static final JSONObject JSON_DOOR_STATUS = new JSONObject();
    public static final JSONObject JSON_GATE_STATUS = new JSONObject();
    public static final JSONObject JSON_SEEK_STREAMING_INDICATOR = new JSONObject();
    public static final JSONArray JSON_GENERAL_SEATS_OCCUPIED = new JSONArray();
    public static final JSONArray JSON_GENERAL_SEATS_BELTED = new JSONArray();
    public static final JSONObject JSON_OBJECT_GENERAL_SEATS_BELTED = new JSONObject();
    public static final JSONObject JSON_OBJECT_GENERAL_SEATS_OCCUPIED = new JSONObject();

    static {
        GENERAL_TOUCHEVENTCAPABILITIES.setDoublePressAvailable(GENERAL_BOOLEAN);
        GENERAL_TOUCHEVENTCAPABILITIES.setMultiTouchAvailable(GENERAL_BOOLEAN);
        GENERAL_TOUCHEVENTCAPABILITIES.setPressAvailable(GENERAL_BOOLEAN);

        GENERAL_SEATMEMORYACTION.setAction(GENERAL_SEATMEMORYACTIONTYPE);
        GENERAL_SEATMEMORYACTION.setLabel(GENERAL_STRING);
        GENERAL_SEATMEMORYACTION.setId(GENERAL_INT);

        GENERAL_IMAGERESOLUTION.setResolutionHeight(GENERAL_INT);
        GENERAL_IMAGERESOLUTION.setResolutionWidth(GENERAL_INT);

        GENERAL_TEMPERATURE.setUnit(GENERAL_TEMPERATUREUNIT);
        GENERAL_TEMPERATURE.setValue(GENERAL_FLOAT);

        GENERAL_CLIMATECONTROLDATA.setFanSpeed(GENERAL_INT);
        GENERAL_CLIMATECONTROLDATA.setCurrentTemperature(GENERAL_TEMPERATURE);
        GENERAL_CLIMATECONTROLDATA.setDesiredTemperature(GENERAL_TEMPERATURE);
        GENERAL_CLIMATECONTROLDATA.setAcEnable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLDATA.setCirculateAirEnable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLDATA.setAutoModeEnable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLDATA.setDualModeEnable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLDATA.setAcMaxEnable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLDATA.setDefrostZone(GENERAL_DEFROSTZONE);
        GENERAL_CLIMATECONTROLDATA.setVentilationMode(GENERAL_VENTILATIONMODE);

        GENERAL_SEATCONTROLDATA.setMemory(GENERAL_SEATMEMORYACTION);
        GENERAL_SEATCONTROLDATA.setMassageCushionFirmness(GENERAL_MASSAGECUSHIONFIRMNESS_LIST);
        GENERAL_SEATCONTROLDATA.setMassageMode(GENERAL_MASSAGEMODEDATA_LIST);
        GENERAL_SEATCONTROLDATA.setMassageEnabled(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLDATA.setHeadSupportHorizontalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setHeadSupportVerticalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setBackTiltAngle(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setBackVerticalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setFrontVerticalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setVerticalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setHorizontalPosition(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setCoolingLevel(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setHeatingLevel(GENERAL_INT);
        GENERAL_SEATCONTROLDATA.setHeatingEnabled(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLDATA.setCoolingEnabled(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLDATA.setId(GENERAL_SUPPORTEDSEAT);

        GENERAL_AUDIOCONTROLDATA.setSource(GENERAL_PRIMARYAUDIOSOURCE);
        GENERAL_AUDIOCONTROLDATA.setVolume(GENERAL_INT);
        GENERAL_AUDIOCONTROLDATA.setKeepContext(GENERAL_BOOLEAN);
        GENERAL_AUDIOCONTROLDATA.setEqualizerSettings(GENERAL_EQUALIZERSETTINGS_LIST);

        GENERAL_HMISETTINGSCONTROLDATA.setDistanceUnit(GENERAL_DISTANCEUNIT);
        GENERAL_HMISETTINGSCONTROLDATA.setTemperatureUnit(GENERAL_TEMPERATUREUNIT);
        GENERAL_HMISETTINGSCONTROLDATA.setDisplayMode(GENERAL_DISPLAYMODE);

        GENERAL_DYNAMICUPDATECAPABILITIES.setSupportsDynamicSubMenus(GENERAL_BOOLEAN);
        GENERAL_DYNAMICUPDATECAPABILITIES.setSupportedDynamicImageFieldNames(Collections.singletonList(GENERAL_IMAGEFIELDNAME));

        GENERAL_LIGHTCONTROLDATA.setLightState(GENERAL_LIGHTSTATE_LIST);

        GENERAL_STATIONIDNUMBER.setCountryCode(GENERAL_INT);
        GENERAL_STATIONIDNUMBER.setFccFacilityId(GENERAL_INT);

        GENERAL_SISDATA.setStationMessage(GENERAL_STRING);
        GENERAL_SISDATA.setStationLocation(VehicleDataHelper.GPS);

        GENERAL_SISDATA.setStationLongName(GENERAL_STRING);
        GENERAL_SISDATA.setStationIDNumber(GENERAL_STATIONIDNUMBER);
        GENERAL_SISDATA.setStationShortName(GENERAL_STRING);

        GENERAL_RDSDATA.setProgramService(GENERAL_STRING);
        GENERAL_RDSDATA.setRadioText(GENERAL_STRING);
        GENERAL_RDSDATA.setClockText(GENERAL_STRING);
        GENERAL_RDSDATA.setProgramIdentification(GENERAL_STRING);
        GENERAL_RDSDATA.setRegion(GENERAL_STRING);
        GENERAL_RDSDATA.setTrafficProgram(GENERAL_BOOLEAN);
        GENERAL_RDSDATA.setTrafficAnnouncement(GENERAL_BOOLEAN);
        GENERAL_RDSDATA.setProgramType(GENERAL_INT);

        GENERAL_RADIOCONTROLDATA.setFrequencyInteger(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setFrequencyFraction(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setBand(GENERAL_RADIOBAND);
        GENERAL_RADIOCONTROLDATA.setRdsData(GENERAL_RDSDATA);
        GENERAL_RADIOCONTROLDATA.setAvailableHDs(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setHdChannel(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setSignalStrength(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setSignalChangeThreshold(GENERAL_INT);
        GENERAL_RADIOCONTROLDATA.setRadioEnable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLDATA.setState(GENERAL_RADIOSTATE);

        GENERAL_MODULEDATA.setModuleType(GENERAL_MODULETYPE);
        GENERAL_MODULEDATA.setClimateControlData(GENERAL_CLIMATECONTROLDATA);

        GENERAL_CHOICE.setMenuName(GENERAL_STRING);
        GENERAL_CHOICE.setSecondaryText(GENERAL_STRING);
        GENERAL_CHOICE.setTertiaryText(GENERAL_STRING);
        GENERAL_CHOICE.setChoiceID(GENERAL_INT);
        GENERAL_CHOICE.setImage(GENERAL_IMAGE);
        GENERAL_CHOICE.setSecondaryImage(GENERAL_IMAGE);
        GENERAL_CHOICE.setVrCommands(GENERAL_STRING_LIST);

        GENERAL_MASSAGEMODEDATA.setMassageMode(GENERAL_MASSAGEMODE);
        GENERAL_MASSAGEMODEDATA.setMassageZone(GENERAL_MASSAGEZONE);
        GENERAL_MASSAGEMODEDATA_LIST.add(GENERAL_MASSAGEMODEDATA);

        GENERAL_MASSAGECUSHIONFIRMNESS.setCushion(GENERAL_MASSAGECUSHION);
        GENERAL_MASSAGECUSHIONFIRMNESS.setFirmness(GENERAL_INT);
        GENERAL_MASSAGECUSHIONFIRMNESS_LIST.add(GENERAL_MASSAGECUSHIONFIRMNESS);

        GENERAL_TOUCHCOORD.setX(GENERAL_INT);
        GENERAL_TOUCHCOORD.setY(GENERAL_INT);
        GENERAL_TOUCHCOORD_LIST.add(GENERAL_TOUCHCOORD);

        GENERAL_TOUCHEVENT.setId(GENERAL_INT);
        GENERAL_TOUCHEVENT.setTs(GENERAL_LONG_LIST);
        GENERAL_TOUCHEVENT.setC(GENERAL_TOUCHCOORD_LIST);
        GENERAL_TOUCHEVENT_LIST.add(GENERAL_TOUCHEVENT);

        GENERAL_TEXTFIELD.setName(GENERAL_TEXTFIELDNAME);
        GENERAL_TEXTFIELD.setRows(GENERAL_INT);
        GENERAL_TEXTFIELD.setWidth(GENERAL_INT);
        GENERAL_TEXTFIELD.setCharacterSet(GENERAL_CHARACTERSET);
        GENERAL_TEXTFIELD_LIST.add(GENERAL_TEXTFIELD);

        GENERAL_COORDINATE.setLongitudeDegrees(GENERAL_FLOAT);
        GENERAL_COORDINATE.setLatitudeDegrees(GENERAL_FLOAT);

        GENERAL_OASISADDRESS.setCountryName(GENERAL_STRING);
        GENERAL_OASISADDRESS.setThoroughfare(GENERAL_STRING);
        GENERAL_OASISADDRESS.setSubThoroughfare(GENERAL_STRING);
        GENERAL_OASISADDRESS.setCountryCode(GENERAL_STRING);
        GENERAL_OASISADDRESS.setPostalCode(GENERAL_STRING);
        GENERAL_OASISADDRESS.setLocality(GENERAL_STRING);
        GENERAL_OASISADDRESS.setSubLocality(GENERAL_STRING);
        GENERAL_OASISADDRESS.setAdministrativeArea(GENERAL_STRING);
        GENERAL_OASISADDRESS.setSubAdministrativeArea(GENERAL_STRING);

        GENERAL_LOCATIONDETAILS.setAddressLines(GENERAL_STRING_LIST);
        GENERAL_LOCATIONDETAILS.setCoordinate(GENERAL_COORDINATE);
        GENERAL_LOCATIONDETAILS.setLocationDescription(GENERAL_STRING);
        GENERAL_LOCATIONDETAILS.setLocationImage(GENERAL_IMAGE);
        GENERAL_LOCATIONDETAILS.setLocationName(GENERAL_STRING);
        GENERAL_LOCATIONDETAILS.setSearchAddress(GENERAL_OASISADDRESS);
        GENERAL_LOCATIONDETAILS.setPhoneNumber(GENERAL_STRING);

        GENERAL_FILETYPE_LIST.add(GENERAL_FILETYPE);

        GENERAL_IMAGEFIELD.setImageResolution(GENERAL_IMAGERESOLUTION);
        GENERAL_IMAGEFIELD.setName(GENERAL_IMAGEFIELDNAME);
        GENERAL_IMAGEFIELD.setImageTypeSupported(GENERAL_FILETYPE_LIST);
        GENERAL_IMAGEFIELD_LIST.add(GENERAL_IMAGEFIELD);

        GENERAL_WINDOW_TYPE_CAPABILITIES.setType(GENERAL_WINDOWTYPE);
        GENERAL_WINDOW_TYPE_CAPABILITIES.setMaximumNumberOfWindows(GENERAL_INT);
        GENERAL_WINDOW_TYPE_CAPABILITIES_LIST.add(GENERAL_WINDOW_TYPE_CAPABILITIES);

        GENERAL_WINDOW_CAPABILITY.setWindowID(GENERAL_INT);
        GENERAL_WINDOW_CAPABILITY.setTextFields(GENERAL_TEXTFIELD_LIST);
        GENERAL_WINDOW_CAPABILITY.setImageFields(GENERAL_IMAGEFIELD_LIST);
        GENERAL_WINDOW_CAPABILITY.setImageTypeSupported(GENERAL_IMAGE_TYPE_LIST);
        GENERAL_WINDOW_CAPABILITY.setTemplatesAvailable(GENERAL_STRING_LIST);
        GENERAL_WINDOW_CAPABILITY.setNumCustomPresetsAvailable(GENERAL_INT);
        GENERAL_WINDOW_CAPABILITY.setButtonCapabilities(GENERAL_BUTTONCAPABILITIES_LIST);
        GENERAL_WINDOW_CAPABILITY.setSoftButtonCapabilities(GENERAL_SOFTBUTTONCAPABILITIES_LIST);
        GENERAL_WINDOW_CAPABILITY_LIST.add(GENERAL_WINDOW_CAPABILITY);

        GENERAL_DISPLAY_CAPABILITY.setDisplayName(GENERAL_STRING);
        GENERAL_DISPLAY_CAPABILITY.setWindowTypeSupported(GENERAL_WINDOW_TYPE_CAPABILITIES_LIST);
        GENERAL_DISPLAY_CAPABILITY.setWindowCapabilities(GENERAL_WINDOW_CAPABILITY_LIST);
        GENERAL_DISPLAYCAPABILITY_LIST.add(GENERAL_DISPLAY_CAPABILITY);

        GENERAL_SCREENPARAMS.setImageResolution(GENERAL_IMAGERESOLUTION);
        GENERAL_SCREENPARAMS.setTouchEventAvailable(GENERAL_TOUCHEVENTCAPABILITIES);

        GENERAL_MEDIACLOCKFORMAT_LIST.add(MediaClockFormat.CLOCK1);
        GENERAL_MEDIACLOCKFORMAT_LIST.add(MediaClockFormat.CLOCK2);
        GENERAL_SEAT_LIST.add(GENERAL_SEATLOCATION);

        GENERAL_IMAGE.setValue(GENERAL_STRING);
        GENERAL_IMAGE.setImageType(GENERAL_IMAGETYPE);
        GENERAL_IMAGE.setIsTemplate(GENERAL_BOOLEAN);

        GENERAL_TEMPLATE_CONFIGURATION.setTemplate(GENERAL_STRING);
        GENERAL_TEMPLATE_CONFIGURATION.setDayColorScheme(TestValues.GENERAL_DAYCOLORSCHEME);
        GENERAL_TEMPLATE_CONFIGURATION.setNightColorScheme(TestValues.GENERAL_NIGHTCOLORSCHEME);

        GENERAL_SOFTBUTTON.setIsHighlighted(GENERAL_BOOLEAN);
        GENERAL_SOFTBUTTON.setSoftButtonID(GENERAL_INT);
        GENERAL_SOFTBUTTON.setSystemAction(SystemAction.STEAL_FOCUS);
        GENERAL_SOFTBUTTON.setText(GENERAL_STRING);
        GENERAL_SOFTBUTTON.setType(SoftButtonType.SBT_TEXT);
        GENERAL_SOFTBUTTON.setImage(GENERAL_IMAGE);
        GENERAL_SOFTBUTTON_LIST.add(GENERAL_SOFTBUTTON);

        GENERAL_TURN.setNavigationText(GENERAL_STRING);
        GENERAL_TURN.setTurnIcon(GENERAL_IMAGE);
        GENERAL_TURN_LIST.add(GENERAL_TURN);

        GENERAL_MENUPARAMS.setMenuName(GENERAL_STRING);
        GENERAL_MENUPARAMS.setParentID(GENERAL_INT);
        GENERAL_MENUPARAMS.setPosition(GENERAL_INT);

        GENERAL_VRHELPITEM.setText(GENERAL_STRING);
        GENERAL_VRHELPITEM.setImage(GENERAL_IMAGE);
        GENERAL_VRHELPITEM.setPosition(100);
        GENERAL_VRHELPITEM_LIST.add(GENERAL_VRHELPITEM);

        GENERAL_TTSCHUNK_LIST.add(new TTSChunk("Welcome to the jungle", SpeechCapabilities.TEXT));
        GENERAL_TTSCHUNK_LIST.add(new TTSChunk("Say a command", SpeechCapabilities.TEXT));

        GENERAL_KEYBOARDPROPERTIES.setAutoCompleteText(GENERAL_STRING);
        GENERAL_KEYBOARDPROPERTIES.setKeypressMode(KeypressMode.SINGLE_KEYPRESS);
        GENERAL_KEYBOARDPROPERTIES.setKeyboardLayout(KeyboardLayout.QWERTY);
        GENERAL_KEYBOARDPROPERTIES.setLanguage(Language.EN_US);
        GENERAL_KEYBOARDPROPERTIES.setLimitedCharacterList(TestValues.GENERAL_STRING_LIST);

        GENERAL_STARTTIME.setHours(GENERAL_INT);
        GENERAL_STARTTIME.setMinutes(GENERAL_INT);
        GENERAL_STARTTIME.setSeconds(GENERAL_INT);

        GENERAL_CHOICE_LIST.add(GENERAL_CHOICE);

        GENERAL_DEVICEINFO.setCarrier(GENERAL_STRING);
        GENERAL_DEVICEINFO.setFirmwareRev(GENERAL_STRING);
        GENERAL_DEVICEINFO.setHardware(GENERAL_STRING);
        GENERAL_DEVICEINFO.setMaxNumberRFCOMMPorts(GENERAL_INT);
        GENERAL_DEVICEINFO.setOs(GENERAL_STRING);
        GENERAL_DEVICEINFO.setOsVersion(GENERAL_STRING);

        GENERAL_SDLMSGVERSION.setMajorVersion(GENERAL_INT);
        GENERAL_SDLMSGVERSION.setMinorVersion(GENERAL_INT);

        GENERAL_APPHMITYPE_LIST.add(AppHMIType.BACKGROUND_PROCESS);
        GENERAL_APPHMITYPE_LIST.add(AppHMIType.COMMUNICATION);

        GENERAL_GLOBALPROPERTY_LIST.add(GlobalProperty.HELPPROMPT);
        GENERAL_GLOBALPROPERTY_LIST.add(GlobalProperty.MENUICON);

        for (VehicleDataType data : VehicleDataType.values()) {
            VehicleDataResult result = new VehicleDataResult();
            result.setResultCode(VehicleDataResultCode.SUCCESS);
            result.setDataType(data);
            GENERAL_VEHICLEDATARESULT_LIST.add(result);
        }

        GENERAL_OEM_CUSTOM_VEHICLE_DATA.setResultCode(VehicleDataResultCode.SUCCESS);
        GENERAL_OEM_CUSTOM_VEHICLE_DATA.setOEMCustomVehicleDataType(GENERAL_OEM_CUSTOM_VEHICLE_DATA_NAME);

        GENERAL_DIDRESULT.setData(GENERAL_STRING);
        GENERAL_DIDRESULT.setDidLocation(GENERAL_INT);
        GENERAL_DIDRESULT.setResultCode(VehicleDataResultCode.SUCCESS);
        GENERAL_DIDRESULT_LIST.add(GENERAL_DIDRESULT);

        GENERAL_DISPLAYCAPABILITIES.setDisplayType(GENERAL_DISPLAYTYPE);
        GENERAL_DISPLAYCAPABILITIES.setDisplayName(GENERAL_STRING);
        GENERAL_DISPLAYCAPABILITIES.setGraphicSupported(GENERAL_BOOLEAN);
        GENERAL_DISPLAYCAPABILITIES.setImageFields(GENERAL_IMAGEFIELD_LIST);
        GENERAL_DISPLAYCAPABILITIES.setMediaClockFormats(GENERAL_MEDIACLOCKFORMAT_LIST);
        GENERAL_DISPLAYCAPABILITIES.setNumCustomPresetsAvailable(GENERAL_INT);
        GENERAL_DISPLAYCAPABILITIES.setScreenParams(GENERAL_SCREENPARAMS);
        GENERAL_DISPLAYCAPABILITIES.setTemplatesAvailable(GENERAL_STRING_LIST);
        GENERAL_DISPLAYCAPABILITIES.setTextFields(GENERAL_TEXTFIELD_LIST);

        GENERAL_PRESETBANKCAPABILITIES.setOnScreenPresetsAvailable(GENERAL_BOOLEAN);

        GENERAL_BUTTONCAPABILITIES.setLongPressAvailable(false);
        GENERAL_BUTTONCAPABILITIES.setShortPressAvailable(true);
        GENERAL_BUTTONCAPABILITIES.setUpDownAvailable(true);
        GENERAL_BUTTONCAPABILITIES.setName(ButtonName.SEEKRIGHT);
        GENERAL_BUTTONCAPABILITIES_LIST.add(GENERAL_BUTTONCAPABILITIES);

        GENERAL_SOFTBUTTONCAPABILITIES.setLongPressAvailable(GENERAL_BOOLEAN);
        GENERAL_SOFTBUTTONCAPABILITIES.setShortPressAvailable(GENERAL_BOOLEAN);
        GENERAL_SOFTBUTTONCAPABILITIES.setUpDownAvailable(GENERAL_BOOLEAN);
        GENERAL_SOFTBUTTONCAPABILITIES.setImageSupported(GENERAL_BOOLEAN);
        GENERAL_SOFTBUTTONCAPABILITIES_LIST.add(GENERAL_SOFTBUTTONCAPABILITIES);

        GENERAL_VEHICLETYPE.setMake(GENERAL_STRING);
        GENERAL_VEHICLETYPE.setModel(GENERAL_STRING);
        GENERAL_VEHICLETYPE.setModelYear(GENERAL_STRING);
        GENERAL_VEHICLETYPE.setTrim(GENERAL_STRING);

        GENERAL_AUDIOPASSTHRUCAPABILITIES.setAudioType(GENERAL_AUDIOTYPE);
        GENERAL_AUDIOPASSTHRUCAPABILITIES.setBitsPerSample(GENERAL_BITSPERSAMPLE);
        GENERAL_AUDIOPASSTHRUCAPABILITIES.setSamplingRate(GENERAL_SAMPLINGRATE);
        GENERAL_AUDIOPASSTHRUCAPABILITIES_LIST.add(GENERAL_AUDIOPASSTHRUCAPABILITIES);

        GENERAL_PRERECORDEDSPEECH_LIST.add(PrerecordedSpeech.HELP_JINGLE);
        GENERAL_PRERECORDEDSPEECH_LIST.add(PrerecordedSpeech.INITIAL_JINGLE);

        GENERAL_HMIZONECAPABILITIES_LIST.add(HmiZoneCapabilities.BACK);
        GENERAL_HMIZONECAPABILITIES_LIST.add(HmiZoneCapabilities.FRONT);

        GENERAL_SPEECHCAPABILITIES_LIST.add(SpeechCapabilities.SILENCE);
        GENERAL_SPEECHCAPABILITIES_LIST.add(SpeechCapabilities.TEXT);

        GENERAL_VRCAPABILITIES_LIST.add(VrCapabilities.TEXT);

        GENERAL_HMIPERMISSIONS.setAllowed(GENERAL_HMILEVEL_LIST);
        GENERAL_HMIPERMISSIONS.setUserDisallowed(GENERAL_HMILEVEL_LIST);

        GENERAL_PARAMETERPERMISSIONS.setAllowed(GENERAL_STRING_LIST);
        GENERAL_PARAMETERPERMISSIONS.setUserDisallowed(GENERAL_STRING_LIST);

        GENERAL_PERMISSIONITEM.setRpcName(GENERAL_STRING);
        GENERAL_PERMISSIONITEM.setHMIPermissions(GENERAL_HMIPERMISSIONS);
        GENERAL_PERMISSIONITEM.setParameterPermissions(GENERAL_PARAMETERPERMISSIONS);
        GENERAL_PERMISSIONITEM.setRequireEncryption(GENERAL_BOOLEAN);
        GENERAL_PERMISSIONITEM_LIST.add(GENERAL_PERMISSIONITEM);

        GENERAL_SYSTEMCAPABILITY.setSystemCapabilityType(GENERAL_SYSTEMCAPABILITYTYPE);

        GENERAL_NAVIGATIONCAPABILITY.setSendLocationEnabled(GENERAL_BOOLEAN);
        GENERAL_NAVIGATIONCAPABILITY.setWayPointsEnabled(GENERAL_BOOLEAN);

        GENERAL_PHONECAPABILITY.setDialNumberEnabled(GENERAL_BOOLEAN);

        GENERAL_VIDEOSTREAMINGFORMAT.setProtocol(GENERAL_VIDEOSTREAMINGPROTOCOL);
        GENERAL_VIDEOSTREAMINGFORMAT.setCodec(GENERAL_VIDEOSTREAMINGCODEC);

        GENERAL_VIDEOSTREAMINGFORMAT_LIST.add(GENERAL_VIDEOSTREAMINGFORMAT);
        GENERAL_VIDEOSTREAMINGFORMAT_LIST.add(GENERAL_VIDEOSTREAMINGFORMAT);

        GENERAL_ADDITIONAL_CAPABILITY.setPreferredResolution(GENERAL_IMAGERESOLUTION);
        GENERAL_ADDITIONAL_CAPABILITY.setDiagonalScreenSize(GENERAL_DOUBLE);
        GENERAL_ADDITIONAL_CAPABILITY.setScale(GENERAL_DOUBLE);

        GENERAL_ADDITIONAL_CAPABILITY_LIST.add(GENERAL_ADDITIONAL_CAPABILITY);
        GENERAL_ADDITIONAL_CAPABILITY.setAdditionalVideoStreamingCapabilities(GENERAL_ADDITIONAL_CAPABILITY_LIST);

        GENERAL_VIDEOSTREAMINGCAPABILITY.setMaxBitrate(GENERAL_INT);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setPreferredResolution(GENERAL_IMAGERESOLUTION);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setSupportedFormats(GENERAL_VIDEOSTREAMINGFORMAT_LIST);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setIsHapticSpatialDataSupported(GENERAL_BOOLEAN);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setDiagonalScreenSize(GENERAL_DOUBLE);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setPixelPerInch(GENERAL_DOUBLE);
        GENERAL_VIDEOSTREAMINGCAPABILITY.setScale(GENERAL_DOUBLE);

        GENERAL_CLIMATECONTROLCAPABILITIES.setModuleName(GENERAL_STRING);
        GENERAL_CLIMATECONTROLCAPABILITIES.setFanSpeedAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setDesiredTemperatureAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setAcEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setAcMaxEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setCirculateAirEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setAutoModeEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setDualModeEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setDefrostZoneAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setDefrostZone(GENERAL_DEFROSTZONE_LIST);
        GENERAL_CLIMATECONTROLCAPABILITIES.setVentilationModeAvailable(GENERAL_BOOLEAN);
        GENERAL_CLIMATECONTROLCAPABILITIES.setVentilationMode(GENERAL_VENTILATIONMODE_LIST);
        GENERAL_CLIMATECONTROLCAPABILITIES_LIST.add(GENERAL_CLIMATECONTROLCAPABILITIES);

        GENERAL_RADIOCONTROLCAPABILITIES.setModuleName(GENERAL_STRING);
        GENERAL_RADIOCONTROLCAPABILITIES.setRadioEnableAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setRadioBandAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setRadioFrequencyAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setHdChannelAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setRdsDataAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setAvailableHDsAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setStateAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setSignalStrengthAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES.setSignalChangeThresholdAvailable(GENERAL_BOOLEAN);
        GENERAL_RADIOCONTROLCAPABILITIES_LIST.add(GENERAL_RADIOCONTROLCAPABILITIES);

        GENERAL_SEATCONTROLCAPABILITIES.setMemoryAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setMassageCushionFirmnessAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setMassageModeAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setMassageEnabledAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setHeadSupportVerticalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setHeadSupportHorizontalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setBackTiltAngleAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setBackVerticalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setFrontVerticalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setVerticalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setHorizontalPositionAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setCoolingLevelAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setHeatingLevelAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setCoolingEnabledAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setHeatingEnabledAvailable(GENERAL_BOOLEAN);
        GENERAL_SEATCONTROLCAPABILITIES.setModuleName(GENERAL_STRING);
        GENERAL_SEATCONTROLCAPABILITIES_LIST.add(GENERAL_SEATCONTROLCAPABILITIES);

        GENERAL_AUDIOCONTROLCAPABILITIES.setEqualizerMaxChannelId(GENERAL_INT);
        GENERAL_AUDIOCONTROLCAPABILITIES.setEqualizerAvailable(GENERAL_BOOLEAN);
        GENERAL_AUDIOCONTROLCAPABILITIES.setVolumeAvailable(GENERAL_BOOLEAN);
        GENERAL_AUDIOCONTROLCAPABILITIES.setSourceAvailable(GENERAL_BOOLEAN);
        GENERAL_AUDIOCONTROLCAPABILITIES.setKeepContextAvailable(GENERAL_BOOLEAN);
        GENERAL_AUDIOCONTROLCAPABILITIES.setModuleName(GENERAL_STRING);
        GENERAL_AUDIOCONTROLCAPABILITIES_LIST.add(GENERAL_AUDIOCONTROLCAPABILITIES);

        GENERAL_HMISETTINGSCONTROLCAPABILITIES.setDisplayModeUnitAvailable(GENERAL_BOOLEAN);
        GENERAL_HMISETTINGSCONTROLCAPABILITIES.setDistanceUnitAvailable(GENERAL_BOOLEAN);
        GENERAL_HMISETTINGSCONTROLCAPABILITIES.setTemperatureUnitAvailable(GENERAL_BOOLEAN);
        GENERAL_HMISETTINGSCONTROLCAPABILITIES.setModuleName(GENERAL_STRING);

        GENERAL_LIGHTCONTROLCAPABILITIES.setSupportedLights(GENERAL_LIGHTCAPABILITIES_LIST);
        GENERAL_LIGHTCONTROLCAPABILITIES.setModuleName(GENERAL_STRING);

        GENERAL_EQUALIZERSETTINGS.setChannelSetting(GENERAL_INT);
        GENERAL_EQUALIZERSETTINGS.setChannelName(GENERAL_STRING);
        GENERAL_EQUALIZERSETTINGS.setChannelId(GENERAL_INT);
        GENERAL_EQUALIZERSETTINGS_LIST.add(GENERAL_EQUALIZERSETTINGS);

        GENERAL_LIGHTCAPABILITIES.setName(GENERAL_LIGHTNAME);
        GENERAL_LIGHTCAPABILITIES.setDensityAvailable(GENERAL_BOOLEAN);
        GENERAL_LIGHTCAPABILITIES.setRGBColorSpaceAvailable(GENERAL_BOOLEAN);
        GENERAL_LIGHTCAPABILITIES_LIST.add(GENERAL_LIGHTCAPABILITIES);

        GENERAL_LIGHTSTATE.setId(GENERAL_LIGHTNAME);
        GENERAL_LIGHTSTATE.setDensity(GENERAL_FLOAT);
        GENERAL_LIGHTSTATE.setStatus(GENERAL_LIGHTSTATUS);
        GENERAL_LIGHTSTATE.setColor(GENERAL_RGBCOLOR);
        GENERAL_LIGHTSTATE_LIST.add(GENERAL_LIGHTSTATE);

        GENERAL_REMOTECONTROLCAPABILITIES.setButtonCapabilities(GENERAL_BUTTONCAPABILITIES_LIST);
        GENERAL_REMOTECONTROLCAPABILITIES.setClimateControlCapabilities(GENERAL_CLIMATECONTROLCAPABILITIES_LIST);
        GENERAL_REMOTECONTROLCAPABILITIES.setRadioControlCapabilities(GENERAL_RADIOCONTROLCAPABILITIES_LIST);

        GENERAL_HMICAPABILITIES.setNavigationAvilable(GENERAL_BOOLEAN);
        GENERAL_HMICAPABILITIES.setVideoStreamingAvailable(GENERAL_BOOLEAN);
        GENERAL_HMICAPABILITIES.setPhoneCallAvilable(GENERAL_BOOLEAN);

        List<MetadataType> exampleList = new ArrayList<>();
        exampleList.add(0, MetadataType.CURRENT_TEMPERATURE);
        exampleList.add(1, MetadataType.MEDIA_ALBUM);
        exampleList.add(2, MetadataType.MEDIA_ARTIST);

        GENERAL_METADATASTRUCT.setMainField1(exampleList);
        GENERAL_METADATASTRUCT.setMainField2(exampleList);
        GENERAL_METADATASTRUCT.setMainField3(exampleList);
        GENERAL_METADATASTRUCT.setMainField4(exampleList);

        GENERAL_RECTANGLE.setX(GENERAL_FLOAT);
        GENERAL_RECTANGLE.setY(GENERAL_FLOAT);
        GENERAL_RECTANGLE.setWidth(GENERAL_FLOAT);
        GENERAL_RECTANGLE.setHeight(GENERAL_FLOAT);

        GENERAL_HAPTIC_RECT.setId(GENERAL_INTEGER);
        GENERAL_HAPTIC_RECT.setRect(GENERAL_RECTANGLE);

        GENERAL_RGBCOLOR.setRed(GENERAL_INTEGER);
        GENERAL_RGBCOLOR.setGreen(GENERAL_INTEGER);
        GENERAL_RGBCOLOR.setBlue(GENERAL_INTEGER);

        GENERAL_NIGHTCOLORSCHEME.setPrimaryColor(GENERAL_RGBCOLOR);
        GENERAL_NIGHTCOLORSCHEME.setSecondaryColor(GENERAL_RGBCOLOR);
        GENERAL_NIGHTCOLORSCHEME.setBackgroundColor(GENERAL_RGBCOLOR);

        GENERAL_DAYCOLORSCHEME.setPrimaryColor(GENERAL_RGBCOLOR);
        GENERAL_DAYCOLORSCHEME.setSecondaryColor(GENERAL_RGBCOLOR);
        GENERAL_DAYCOLORSCHEME.setBackgroundColor(GENERAL_RGBCOLOR);

        GENERAL_LOCKSCREENCONFIG.setAppIcon(R.drawable.sdl_lockscreen_icon);
        GENERAL_LOCKSCREENCONFIG.setBackgroundColor(Color.BLUE);
        GENERAL_LOCKSCREENCONFIG.setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
        GENERAL_LOCKSCREENCONFIG.setCustomView(R.layout.activity_sdllock_screen);
        GENERAL_CLOUDAPPPROPERTIES.setNicknames(GENERAL_STRING_LIST);
        GENERAL_CLOUDAPPPROPERTIES.setAppID(GENERAL_STRING);
        GENERAL_CLOUDAPPPROPERTIES.setEnabled(GENERAL_BOOLEAN);
        GENERAL_CLOUDAPPPROPERTIES.setAuthToken(GENERAL_STRING);
        GENERAL_CLOUDAPPPROPERTIES.setCloudTransportType(GENERAL_STRING);
        GENERAL_CLOUDAPPPROPERTIES.setHybridAppPreference(GENERAL_HYBRID_APP_PREFERENCE);
        GENERAL_CLOUDAPPPROPERTIES.setEndpoint(GENERAL_STRING);
        GENERAL_WEATHER_SERVICE_MANIFEST.setWeatherForLocationSupported(GENERAL_BOOLEAN);
        GENERAL_WEATHER_SERVICE_MANIFEST.setCurrentForecastSupported(GENERAL_BOOLEAN);
        GENERAL_WEATHER_SERVICE_MANIFEST.setMaxMultidayForecastAmount(GENERAL_INTEGER);
        GENERAL_WEATHER_SERVICE_MANIFEST.setMaxMinutelyForecastAmount(GENERAL_INTEGER);
        GENERAL_WEATHER_SERVICE_MANIFEST.setMaxHourlyForecastAmount(GENERAL_INTEGER);

        GENERAL_APPSERVICEMANIFEST.setWeatherServiceManifest(GENERAL_WEATHER_SERVICE_MANIFEST);
        GENERAL_APPSERVICEMANIFEST.setServiceName(GENERAL_STRING);
        GENERAL_APPSERVICEMANIFEST.setServiceIcon(GENERAL_IMAGE);
        GENERAL_APPSERVICEMANIFEST.setRpcSpecVersion(GENERAL_SDLMSGVERSION);
        GENERAL_APPSERVICEMANIFEST.setMediaServiceManifest(GENERAL_MEDIA_SERVICE_MANIFEST);
        GENERAL_APPSERVICEMANIFEST.setHandledRpcs(GENERAL_FUNCTION_ID_LIST);
        GENERAL_APPSERVICEMANIFEST.setAllowAppConsumers(GENERAL_BOOLEAN);
        GENERAL_APPSERVICEMANIFEST.setServiceType(GENERAL_STRING);

        GENERAL_NAVIGATION_SERVICE_MANIFEST.setAcceptsWayPoints(GENERAL_BOOLEAN);

        GENERAL_APPSERVICERECORD.setServiceID(GENERAL_STRING);
        GENERAL_APPSERVICERECORD.setServiceManifest(GENERAL_APPSERVICEMANIFEST);
        GENERAL_APPSERVICERECORD.setServiceActive(GENERAL_BOOLEAN);
        GENERAL_APPSERVICERECORD.setServicePublished(GENERAL_BOOLEAN);

        GENERAL_APP_SERVICE_CAPABILITY.setUpdatedAppServiceRecord(GENERAL_APPSERVICERECORD);
        GENERAL_APP_SERVICE_CAPABILITY.setUpdateReason(GENERAL_SERVICE_UPDATE_REASON);

        GENERAL_APP_SERVICE_CAPABILITIES.setAppServices(GENERAL_APPSERVICECAPABILITY_LIST);

        GENERAL_DATETIME.setDay(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setHour(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setMilliSecond(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setMinute(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setMonth(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setSecond(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setTzHour(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setTzMinute(TestValues.GENERAL_INT);
        GENERAL_DATETIME.setYear(TestValues.GENERAL_INT);

        GENERAL_WEATHERDATA.setCurrentTemperature(GENERAL_TEMPERATURE);
        GENERAL_WEATHERDATA.setTemperatureHigh(GENERAL_TEMPERATURE);
        GENERAL_WEATHERDATA.setTemperatureLow(GENERAL_TEMPERATURE);
        GENERAL_WEATHERDATA.setApparentTemperature(GENERAL_TEMPERATURE);
        GENERAL_WEATHERDATA.setWeatherSummary(GENERAL_STRING);
        GENERAL_WEATHERDATA.setTime(GENERAL_DATETIME);
        GENERAL_WEATHERDATA.setHumidity(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setCloudCover(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setMoonPhase(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setWindBearing(GENERAL_INTEGER);
        GENERAL_WEATHERDATA.setWindGust(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setWindSpeed(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setNearestStormBearing(GENERAL_INTEGER);
        GENERAL_WEATHERDATA.setNearestStormDistance(GENERAL_INTEGER);
        GENERAL_WEATHERDATA.setPrecipAccumulation(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setPrecipIntensity(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setPrecipProbability(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setPrecipType(GENERAL_STRING);
        GENERAL_WEATHERDATA.setVisibility(GENERAL_FLOAT);
        GENERAL_WEATHERDATA.setWeatherIcon(GENERAL_IMAGE);

        GENERAL_WEATHERALERT.setTitle(GENERAL_STRING);
        GENERAL_WEATHERALERT.setSummary(GENERAL_STRING);
        GENERAL_WEATHERALERT.setExpires(GENERAL_DATETIME);
        GENERAL_WEATHERALERT.setRegions(GENERAL_STRING_LIST);
        GENERAL_WEATHERALERT.setSeverity(GENERAL_STRING);
        GENERAL_WEATHERALERT.setTimeIssued(GENERAL_DATETIME);

        GENERAL_WEATHERSERVICEDATA.setLocation(GENERAL_LOCATIONDETAILS);
        GENERAL_WEATHERSERVICEDATA.setCurrentForecast(GENERAL_WEATHERDATA);
        GENERAL_WEATHERSERVICEDATA.setMinuteForecast(GENERAL_WEATHERDATA_LIST);
        GENERAL_WEATHERSERVICEDATA.setHourlyForecast(GENERAL_WEATHERDATA_LIST);
        GENERAL_WEATHERSERVICEDATA.setMultidayForecast(GENERAL_WEATHERDATA_LIST);
        GENERAL_WEATHERSERVICEDATA.setAlerts(GENERAL_WEATHERALERT_LIST);

        GENERAL_MEDIASERVICEDATA.setMediaType(GENERAL_MEDIATYPE);
        GENERAL_MEDIASERVICEDATA.setMediaTitle(GENERAL_STRING);
        GENERAL_MEDIASERVICEDATA.setMediaArtist(GENERAL_STRING);
        GENERAL_MEDIASERVICEDATA.setMediaAlbum(GENERAL_STRING);
        GENERAL_MEDIASERVICEDATA.setPlaylistName(GENERAL_STRING);
        GENERAL_MEDIASERVICEDATA.setIsExplicit(GENERAL_BOOLEAN);
        GENERAL_MEDIASERVICEDATA.setTrackPlaybackProgress(GENERAL_INTEGER);
        GENERAL_MEDIASERVICEDATA.setTrackPlaybackDuration(GENERAL_INTEGER);
        GENERAL_MEDIASERVICEDATA.setQueuePlaybackProgress(GENERAL_INTEGER);
        GENERAL_MEDIASERVICEDATA.setQueuePlaybackDuration(GENERAL_INTEGER);
        GENERAL_MEDIASERVICEDATA.setQueueCurrentTrackNumber(GENERAL_INTEGER);
        GENERAL_MEDIASERVICEDATA.setQueueTotalTrackCount(GENERAL_INTEGER);

        GENERAL_APPSERVICEDATA.setServiceType(GENERAL_STRING);
        GENERAL_APPSERVICEDATA.setServiceID(GENERAL_STRING);
        GENERAL_APPSERVICEDATA.setWeatherServiceData(GENERAL_WEATHERSERVICEDATA);
        GENERAL_APPSERVICEDATA.setMediaServiceData(GENERAL_MEDIASERVICEDATA);

        GENERAL_NAVIGATION_INSTRUCTION.setLocationDetails(GENERAL_LOCATIONDETAILS);
        GENERAL_NAVIGATION_INSTRUCTION.setAction(GENERAL_NAVIGATIONACTION);
        GENERAL_NAVIGATION_INSTRUCTION.setEta(GENERAL_DATETIME);
        GENERAL_NAVIGATION_INSTRUCTION.setBearing(GENERAL_INTEGER);
        GENERAL_NAVIGATION_INSTRUCTION.setJunctionType(GENERAL_NAVIGATION_JUNCTION);
        GENERAL_NAVIGATION_INSTRUCTION.setDrivingSide(GENERAL_DIRECTION);
        GENERAL_NAVIGATION_INSTRUCTION.setDetails(GENERAL_STRING);
        GENERAL_NAVIGATION_INSTRUCTION.setImage(GENERAL_IMAGE);
        GENERAL_MODULE_INFO.setModuleId(TestValues.GENERAL_STRING);
        GENERAL_MODULE_INFO.setModuleLocation(TestValues.GENERAL_GRID);
        GENERAL_MODULE_INFO.setModuleServiceArea(TestValues.GENERAL_GRID);
        GENERAL_MODULE_INFO.setMultipleAccessAllowance(TestValues.GENERAL_BOOLEAN);

        GENERAL_GEAR_STATUS.setActualGear(GENERAL_ACTUAL_GEAR);
        GENERAL_GEAR_STATUS.setTransmissionType(GENERAL_TRANSMISSION_TYPE);
        GENERAL_GEAR_STATUS.setUserSelectedGear(GENERAL_USER_SELECTED_GEAR);

        GENERAL_WINDOW_STATUS.setLocation(TestValues.GENERAL_GRID);
        GENERAL_WINDOW_STATUS.setState(TestValues.GENERAL_WINDOW_STATE);
        GENERAL_STABILITY_CONTROL_STATUS.setEscSystem(GENERAL_ESC_SYSTEM);
        GENERAL_STABILITY_CONTROL_STATUS.setTrailerSwayControl(GENERAL_S_WAY_CONTROL);

        GENERAL_SUPPORTED_KEYBOARDS_LIST.add(GENERAL_KEYBOARD_LAYOUT_CAPABILITY);

        GENERAL_KEYBOARD_CAPABILITIES.setMaskInputCharactersSupported(TestValues.GENERAL_BOOLEAN);
        GENERAL_KEYBOARD_CAPABILITIES.setSupportedKeyboards(GENERAL_SUPPORTED_KEYBOARDS_LIST);

        GENERAL_APPCAPABILITY.setVideoStreamingCapability(GENERAL_VIDEOSTREAMINGCAPABILITY);
        GENERAL_APPCAPABILITY.setAppCapabilityType(GENERAL_APPCAPABILITYTYPE);

        // SEAT_OCCUPANCY
        GENERAL_SEATLOCATION.setGrid(GENERAL_LOCATION_GRID);

        GENERAL_SEAT_STATUS.setConditionActive(GENERAL_BOOLEAN);
        GENERAL_SEAT_STATUS.setSeatLocation(GENERAL_SEATLOCATION);

        GENERAL_SEATS_BELTED.add(GENERAL_SEAT_STATUS);
        GENERAL_SEATS_OCCUPIED.add(GENERAL_SEAT_STATUS);

        GENERAL_SEAT_OCCUPANCY.setSeatsBelted(GENERAL_SEATS_BELTED);
        GENERAL_SEAT_OCCUPANCY.setSeatsOccupied(GENERAL_SEATS_OCCUPIED);

        GENERAL_ROOF_STATUS.setLocation(GENERAL_GRID);
        GENERAL_ROOF_STATUS.setState(GENERAL_WINDOW_STATE);
        GENERAL_ROOF_STATUS.setStatus(GENERAL_DOORSTATUSTYPE);
        GENERAL_ROOF_STATUS_LIST.add(GENERAL_ROOF_STATUS);

        GENERAL_GATE_STATUS.setLocation(GENERAL_GRID);
        GENERAL_GATE_STATUS.setStatus(GENERAL_DOORSTATUSTYPE);
        GENERAL_GATE_STATUS_LIST.add(GENERAL_GATE_STATUS);

        GENERAL_DOOR_STATUS.setLocation(GENERAL_GRID);
        GENERAL_DOOR_STATUS.setStatus(GENERAL_DOORSTATUSTYPE);
        GENERAL_DOOR_STATUS_LIST.add(GENERAL_DOOR_STATUS);

        // Climate Data
        GENERAL_CLIMATEDATA.setAtmosphericPressure(GENERAL_FLOAT);
        GENERAL_CLIMATEDATA.setCabinTemperature(GENERAL_TEMPERATURE);
        GENERAL_CLIMATEDATA.setExternalTemperature(GENERAL_TEMPERATURE);

        try {

            for (KeyboardLayoutCapability keyboard : GENERAL_SUPPORTED_KEYBOARDS_LIST) {
                JSON_SUPPORTED_KEYBOARDS_LIST.put(keyboard.serializeJSON());
            }

            JSON_KEYBOARD_CAPABILITY.put(KeyboardCapabilities.KEY_MASK_INPUT_CHARACTERS_SUPPORTED, GENERAL_BOOLEAN);
            JSON_KEYBOARD_CAPABILITY.put(KeyboardCapabilities.KEY_SUPPORTED_KEYBOARDS, JSON_SUPPORTED_KEYBOARDS_LIST);


            JSON_OBJECT_GENERAL_SEATS_OCCUPIED.put(SeatStatus.KEY_SEAT_LOCATION, GENERAL_SEATLOCATION.serializeJSON());
            JSON_OBJECT_GENERAL_SEATS_OCCUPIED.put(SeatStatus.KEY_CONDITION_ACTIVE, GENERAL_BOOLEAN);
            JSON_GENERAL_SEATS_OCCUPIED.put(JSON_OBJECT_GENERAL_SEATS_OCCUPIED);

            JSON_OBJECT_GENERAL_SEATS_BELTED.put(SeatStatus.KEY_SEAT_LOCATION, GENERAL_SEATLOCATION.serializeJSON());
            JSON_OBJECT_GENERAL_SEATS_BELTED.put(SeatStatus.KEY_CONDITION_ACTIVE,GENERAL_BOOLEAN);
            JSON_GENERAL_SEATS_BELTED.put(JSON_OBJECT_GENERAL_SEATS_BELTED);

            JSON_HMIPERMISSIONS.put(HMIPermissions.KEY_ALLOWED, GENERAL_HMILEVEL_LIST);
            JSON_HMIPERMISSIONS.put(HMIPermissions.KEY_USER_DISALLOWED, GENERAL_HMILEVEL_LIST);

            JSON_PCMSTREAMCAPABILITIES.put(AudioPassThruCapabilities.KEY_AUDIO_TYPE, GENERAL_AUDIOTYPE);
            JSON_PCMSTREAMCAPABILITIES.put(AudioPassThruCapabilities.KEY_BITS_PER_SAMPLE, GENERAL_BITSPERSAMPLE);
            JSON_PCMSTREAMCAPABILITIES.put(AudioPassThruCapabilities.KEY_SAMPLING_RATE, GENERAL_SAMPLINGRATE);

            JSON_TOUCHEVENTCAPABILITIES.put(TouchEventCapabilities.KEY_DOUBLE_PRESS_AVAILABLE, GENERAL_BOOLEAN);
            JSON_TOUCHEVENTCAPABILITIES.put(TouchEventCapabilities.KEY_MULTI_TOUCH_AVAILABLE, GENERAL_BOOLEAN);
            JSON_TOUCHEVENTCAPABILITIES.put(TouchEventCapabilities.KEY_PRESS_AVAILABLE, GENERAL_BOOLEAN);

            JSON_IMAGERESOLUTION.put(ImageResolution.KEY_RESOLUTION_HEIGHT, GENERAL_INT);
            JSON_IMAGERESOLUTION.put(ImageResolution.KEY_RESOLUTION_WIDTH, GENERAL_INT);

            JSON_CHOICE.put(Choice.KEY_MENU_NAME, GENERAL_STRING);
            JSON_CHOICE.put(Choice.KEY_SECONDARY_TEXT, GENERAL_STRING);
            JSON_CHOICE.put(Choice.KEY_TERTIARY_TEXT, GENERAL_STRING);
            JSON_CHOICE.put(Choice.KEY_CHOICE_ID, GENERAL_INT);
            JSON_CHOICE.put(Choice.KEY_IMAGE, JSON_IMAGE);
            JSON_CHOICE.put(Choice.KEY_SECONDARY_IMAGE, JSON_IMAGE);
            JSON_CHOICE.put(Choice.KEY_VR_COMMANDS, JsonUtils.createJsonArray(GENERAL_STRING_LIST));

            JSON_IMAGE_TYPE_SUPPORTED.put(ImageType.DYNAMIC);
            JSON_IMAGE_TYPE_SUPPORTED.put(ImageType.STATIC);

            JSON_HMILEVELS.put(HMILevel.HMI_FULL);
            JSON_HMILEVELS.put(HMILevel.HMI_BACKGROUND);

            JSON_HMIPERMISSIONS.put(HMIPermissions.KEY_ALLOWED, JSON_HMILEVELS);
            JSON_HMIPERMISSIONS.put(HMIPermissions.KEY_USER_DISALLOWED, JSON_HMILEVELS);

            JSON_PARAMETERPERMISSIONS.put(ParameterPermissions.KEY_ALLOWED, JsonUtils.createJsonArray(GENERAL_STRING_LIST));
            JSON_PARAMETERPERMISSIONS.put(ParameterPermissions.KEY_USER_DISALLOWED, JsonUtils.createJsonArray(GENERAL_STRING_LIST));

            JSON_PERMISSIONITEM.put(PermissionItem.KEY_HMI_PERMISSIONS, JSON_HMIPERMISSIONS);
            JSON_PERMISSIONITEM.put(PermissionItem.KEY_PARAMETER_PERMISSIONS, JSON_PARAMETERPERMISSIONS);
            JSON_PERMISSIONITEM.put(PermissionItem.KEY_RPC_NAME, GENERAL_STRING);
            JSON_PERMISSIONITEM.put(PermissionItem.KEY_REQUIRE_ENCRYPTION, GENERAL_BOOLEAN);
            JSON_PERMISSIONITEMS.put(JSON_PERMISSIONITEM);

            JSON_IMAGE.put(Image.KEY_IMAGE_TYPE, GENERAL_IMAGETYPE);
            JSON_IMAGE.put(Image.KEY_VALUE, GENERAL_STRING);
            JSON_IMAGE.put(Image.KEY_IS_TEMPLATE, GENERAL_BOOLEAN);

            JSON_SOFTBUTTON.put(SoftButton.KEY_IS_HIGHLIGHTED, GENERAL_BOOLEAN);
            JSON_SOFTBUTTON.put(SoftButton.KEY_SOFT_BUTTON_ID, GENERAL_INT);
            JSON_SOFTBUTTON.put(SoftButton.KEY_SYSTEM_ACTION, SystemAction.STEAL_FOCUS);
            JSON_SOFTBUTTON.put(SoftButton.KEY_TEXT, GENERAL_STRING);
            JSON_SOFTBUTTON.put(SoftButton.KEY_TYPE, SoftButtonType.SBT_TEXT);
            JSON_SOFTBUTTON.put(SoftButton.KEY_IMAGE, GENERAL_IMAGE.serializeJSON());
            JSON_SOFTBUTTONS.put(JSON_SOFTBUTTON);

            JSON_TURN.put(Turn.KEY_NAVIGATION_TEXT, GENERAL_STRING);
            JSON_TURN.put(Turn.KEY_TURN_IMAGE, GENERAL_IMAGE.serializeJSON());
            JSON_TURNS.put(JSON_TURN);

            JSON_MENUPARAMS.put(MenuParams.KEY_MENU_NAME, GENERAL_STRING);
            JSON_MENUPARAMS.put(MenuParams.KEY_PARENT_ID, GENERAL_INT);
            JSON_MENUPARAMS.put(MenuParams.KEY_POSITION, GENERAL_INT);

            JSON_VRHELPITEM.put(VrHelpItem.KEY_TEXT, GENERAL_STRING);
            JSON_VRHELPITEM.put(VrHelpItem.KEY_IMAGE, JSON_IMAGE);
            JSON_VRHELPITEM.put(VrHelpItem.KEY_POSITION, GENERAL_INT);
            JSON_VRHELPITEMS.put(JSON_VRHELPITEM);

            JSONObject jsonTtsChunk = new JSONObject();
            jsonTtsChunk.put(TTSChunk.KEY_TEXT, "Welcome to the jungle");
            jsonTtsChunk.put(TTSChunk.KEY_TYPE, SpeechCapabilities.TEXT);
            JSON_TTSCHUNKS.put(jsonTtsChunk);
            jsonTtsChunk = new JSONObject();
            jsonTtsChunk.put(TTSChunk.KEY_TEXT, "Say a command");
            jsonTtsChunk.put(TTSChunk.KEY_TYPE, SpeechCapabilities.TEXT);
            JSON_TTSCHUNKS.put(jsonTtsChunk);

            JSON_KEYBOARDPROPERTIES.put(KeyboardProperties.KEY_AUTO_COMPLETE_TEXT, GENERAL_STRING);
            JSON_KEYBOARDPROPERTIES.put(KeyboardProperties.KEY_KEYPRESS_MODE, KeypressMode.SINGLE_KEYPRESS);
            JSON_KEYBOARDPROPERTIES.put(KeyboardProperties.KEY_KEYBOARD_LAYOUT, KeyboardLayout.QWERTY);
            JSON_KEYBOARDPROPERTIES.put(KeyboardProperties.KEY_LANGUAGE, Language.EN_US);
            JSON_KEYBOARDPROPERTIES.put(KeyboardProperties.KEY_LIMITED_CHARACTER_LIST, JsonUtils.createJsonArray(GENERAL_STRING_LIST));

            JSON_STARTTIME.put(StartTime.KEY_HOURS, GENERAL_STARTTIME.getHours());
            JSON_STARTTIME.put(StartTime.KEY_MINUTES, GENERAL_STARTTIME.getMinutes());
            JSON_STARTTIME.put(StartTime.KEY_SECONDS, GENERAL_STARTTIME.getSeconds());

            JSON_CHOICES.put(JSON_CHOICE);

            JSON_DEVICEINFO.put(DeviceInfo.KEY_CARRIER, GENERAL_STRING);
            JSON_DEVICEINFO.put(DeviceInfo.KEY_FIRMWARE_REV, GENERAL_STRING);
            JSON_DEVICEINFO.put(DeviceInfo.KEY_HARDWARE, GENERAL_STRING);
            JSON_DEVICEINFO.put(DeviceInfo.KEY_MAX_NUMBER_RFCOMM_PORTS, GENERAL_INT);
            JSON_DEVICEINFO.put(DeviceInfo.KEY_OS, GENERAL_STRING);
            JSON_DEVICEINFO.put(DeviceInfo.KEY_OS_VERSION, GENERAL_STRING);

            JSON_RGBCOLOR.put(RGBColor.KEY_RED, GENERAL_INT);
            JSON_RGBCOLOR.put(RGBColor.KEY_GREEN, GENERAL_INT);
            JSON_RGBCOLOR.put(RGBColor.KEY_BLUE, GENERAL_INT);

            JSON_DAYCOLORSCHEME.put(TemplateColorScheme.KEY_PRIMARY_COLOR, JSON_RGBCOLOR);
            JSON_DAYCOLORSCHEME.put(TemplateColorScheme.KEY_SECONDARY_COLOR, JSON_RGBCOLOR);
            JSON_DAYCOLORSCHEME.put(TemplateColorScheme.KEY_BACKGROUND_COLOR, JSON_RGBCOLOR);

            JSON_NIGHTCOLORSCHEME.put(TemplateColorScheme.KEY_PRIMARY_COLOR, JSON_RGBCOLOR);
            JSON_NIGHTCOLORSCHEME.put(TemplateColorScheme.KEY_SECONDARY_COLOR, JSON_RGBCOLOR);
            JSON_NIGHTCOLORSCHEME.put(TemplateColorScheme.KEY_BACKGROUND_COLOR, JSON_RGBCOLOR);

            JSON_SDLMSGVERSION.put(SdlMsgVersion.KEY_MAJOR_VERSION, GENERAL_INT);
            JSON_SDLMSGVERSION.put(SdlMsgVersion.KEY_MINOR_VERSION, GENERAL_INT);

            JSON_DIDRESULT.put(DIDResult.KEY_DATA, GENERAL_STRING);
            JSON_DIDRESULT.put(DIDResult.KEY_DID_LOCATION, GENERAL_INT);
            JSON_DIDRESULT.put(DIDResult.KEY_RESULT_CODE, VehicleDataResultCode.SUCCESS);
            JSON_DIDRESULTS.put(JSON_DIDRESULT);

            JSON_PRESETBANKCAPABILITIES.put(PresetBankCapabilities.KEY_ON_SCREEN_PRESETS_AVAILABLE, GENERAL_BOOLEAN);

            JSONObject jsonButton = new JSONObject();
            jsonButton.put(ButtonCapabilities.KEY_LONG_PRESS_AVAILABLE, false);
            jsonButton.put(ButtonCapabilities.KEY_SHORT_PRESS_AVAILABLE, GENERAL_BOOLEAN);
            jsonButton.put(ButtonCapabilities.KEY_UP_DOWN_AVAILABLE, GENERAL_BOOLEAN);
            jsonButton.put(ButtonCapabilities.KEY_NAME, ButtonName.SEEKRIGHT);
            JSON_BUTTONCAPABILITIES.put(jsonButton);

            JSONObject jsonRadioControlCapabilities = new JSONObject();
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_MODULE_NAME, GENERAL_STRING);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_RADIO_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_RADIO_BAND_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_RADIO_FREQUENCY_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_HD_CHANNEL_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_RDS_DATA_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_AVAILABLE_HDS_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_STATE_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_SIGNAL_STRENGTH_AVAILABLE, GENERAL_BOOLEAN);
            jsonRadioControlCapabilities.put(RadioControlCapabilities.KEY_SIGNAL_CHANGE_THRESHOLD_AVAILABLE, GENERAL_BOOLEAN);
            JSON_RADIOCONTROLCAPABILITIES.put(jsonRadioControlCapabilities);

            JSONObject jsonClimateControlCapabilities = new JSONObject();
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_MODULE_NAME, GENERAL_STRING);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_FAN_SPEED_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_DESIRED_TEMPERATURE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_AC_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_AC_MAX_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_CIRCULATE_AIR_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_AUTO_MODE_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_DUAL_MODE_ENABLE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_DEFROST_ZONE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_DEFROST_ZONE, GENERAL_DEFROSTZONE_LIST);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_VENTILATION_MODE_AVAILABLE, GENERAL_BOOLEAN);
            jsonClimateControlCapabilities.put(ClimateControlCapabilities.KEY_VENTILATION_MODE, GENERAL_VENTILATIONMODE_LIST);
            JSON_CLIMATECONTROLCAPABILITIES.put(jsonClimateControlCapabilities);

            jsonButton = new JSONObject();
            jsonButton.put(SoftButtonCapabilities.KEY_LONG_PRESS_AVAILABLE, GENERAL_BOOLEAN);
            jsonButton.put(SoftButtonCapabilities.KEY_SHORT_PRESS_AVAILABLE, GENERAL_BOOLEAN);
            jsonButton.put(SoftButtonCapabilities.KEY_UP_DOWN_AVAILABLE, GENERAL_BOOLEAN);
            jsonButton.put(SoftButtonCapabilities.KEY_IMAGE_SUPPORTED, GENERAL_BOOLEAN);
            JSON_SOFTBUTTONCAPABILITIES.put(jsonButton);

            jsonButton = new JSONObject();
            jsonButton.put(AudioPassThruCapabilities.KEY_AUDIO_TYPE, GENERAL_AUDIOTYPE);
            jsonButton.put(AudioPassThruCapabilities.KEY_BITS_PER_SAMPLE, GENERAL_BITSPERSAMPLE);
            jsonButton.put(AudioPassThruCapabilities.KEY_SAMPLING_RATE, GENERAL_SAMPLINGRATE);
            JSON_AUDIOPASSTHRUCAPABILITIES.put(jsonButton);

            JSON_TEXTFIELD.put(TextField.KEY_CHARACTER_SET, CharacterSet.CID1SET);
            JSON_TEXTFIELD.put(TextField.KEY_NAME, TextFieldName.ETA);
            JSON_TEXTFIELD.put(TextField.KEY_ROWS, GENERAL_INT);
            JSON_TEXTFIELD.put(TextField.KEY_WIDTH, GENERAL_INT);
            JSON_TEXTFIELDS.put(JSON_TEXTFIELD);

            JSON_IMAGEFIELD.put(ImageField.KEY_IMAGE_RESOLUTION, JSON_IMAGERESOLUTION);
            JSON_IMAGEFIELD.put(ImageField.KEY_IMAGE_TYPE_SUPPORTED, JsonUtils.createJsonArray(TestValues.GENERAL_FILETYPE_LIST));
            JSON_IMAGEFIELD.put(ImageField.KEY_NAME, ImageFieldName.graphic);
            JSON_IMAGEFIELDS.put(JSON_IMAGEFIELD);

            JSON_WINDOW_TYPE_CAPABILITIES.put(WindowTypeCapabilities.KEY_TYPE, GENERAL_WINDOWTYPE);
            JSON_WINDOW_TYPE_CAPABILITIES.put(WindowTypeCapabilities.KEY_MAXIMUM_NUMBER_OF_WINDOWS, GENERAL_INT);
            JSON_WINDOW_TYPE_CAPABILITIES_LIST.put(JSON_WINDOW_TYPE_CAPABILITIES);

            JSON_IMAGE_TYPES.put(ImageType.DYNAMIC);
            JSON_IMAGE_TYPES.put(ImageType.STATIC);

            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_WINDOW_ID, GENERAL_INT);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_TEXT_FIELDS, JSON_TEXTFIELDS);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_IMAGE_FIELDS, JSON_IMAGEFIELDS);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_IMAGE_TYPE_SUPPORTED, JSON_IMAGE_TYPES);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_TEMPLATES_AVAILABLE, JsonUtils.createJsonArray(GENERAL_STRING_LIST));
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_NUM_CUSTOM_PRESETS_AVAILABLE, GENERAL_INT);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_BUTTON_CAPABILITIES, JSON_BUTTONCAPABILITIES);
            JSON_WINDOW_CAPABILITY.put(WindowCapability.KEY_SOFT_BUTTON_CAPABILITIES, JSON_SOFTBUTTONCAPABILITIES);
            JSON_WINDOW_CAPABILITIES.put(JSON_WINDOW_CAPABILITY);

            JSONObject jsonTEC = new JSONObject();
            jsonTEC.put(TouchEventCapabilities.KEY_DOUBLE_PRESS_AVAILABLE, GENERAL_BOOLEAN);
            jsonTEC.put(TouchEventCapabilities.KEY_MULTI_TOUCH_AVAILABLE, GENERAL_BOOLEAN);
            jsonTEC.put(TouchEventCapabilities.KEY_PRESS_AVAILABLE, GENERAL_BOOLEAN);

            JSON_SCREENPARAMS.put(ScreenParams.KEY_RESOLUTION, JSON_IMAGERESOLUTION);
            JSON_SCREENPARAMS.put(ScreenParams.KEY_TOUCH_EVENT_AVAILABLE, jsonTEC);

            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_DISPLAY_TYPE, GENERAL_DISPLAYTYPE);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_DISPLAY_NAME, GENERAL_STRING);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_GRAPHIC_SUPPORTED, GENERAL_BOOLEAN);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_IMAGE_FIELDS, JSON_IMAGEFIELDS);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_MEDIA_CLOCK_FORMATS, JsonUtils.createJsonArray(GENERAL_MEDIACLOCKFORMAT_LIST));
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_NUM_CUSTOM_PRESETS_AVAILABLE, GENERAL_INT);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_SCREEN_PARAMS, JSON_SCREENPARAMS);
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_TEMPLATES_AVAILABLE, JsonUtils.createJsonArray(GENERAL_STRING_LIST));
            JSON_DISPLAYCAPABILITIES.put(DisplayCapabilities.KEY_TEXT_FIELDS, JSON_TEXTFIELDS);

            JSON_DISPLAYCAPABILITY.put(DisplayCapability.KEY_DISPLAY_NAME, GENERAL_STRING);
            JSON_DISPLAYCAPABILITY.put(DisplayCapability.KEY_WINDOW_TYPE_SUPPORTED, JSON_WINDOW_TYPE_CAPABILITIES_LIST);
            JSON_DISPLAYCAPABILITY.put(DisplayCapability.KEY_WINDOW_CAPABILITIES, JSON_WINDOW_CAPABILITIES);
            JSON_DISPLAYCAPABILITY_LIST.put(JSON_DISPLAYCAPABILITY);

            JSON_DYNAMICUPDATECAPABILITIES.put(DynamicUpdateCapabilities.KEY_SUPPORTS_DYNAMIC_SUB_MENUS, GENERAL_BOOLEAN);
            JSON_DYNAMICUPDATECAPABILITIES.put(DynamicUpdateCapabilities.KEY_SUPPORTED_DYNAMIC_IMAGE_FIELD_NAMES, JsonUtils.createJsonArray(Collections.singletonList(GENERAL_IMAGEFIELDNAME)));

            JSON_TOUCHCOORD.put(TouchCoord.KEY_X, GENERAL_INT);
            JSON_TOUCHCOORD.put(TouchCoord.KEY_Y, GENERAL_INT);
            JSON_TOUCHCOORDS.put(JSON_TOUCHCOORD);

            JSON_TOUCHEVENT.put(TouchEvent.KEY_C, JSON_TOUCHCOORDS);
            JSON_TOUCHEVENT.put(TouchEvent.KEY_ID, GENERAL_INT);
            JSON_TOUCHEVENT.put(TouchEvent.KEY_TS, JsonUtils.createJsonArray(GENERAL_LONG_LIST));
            JSON_TOUCHEVENTS.put(JSON_TOUCHEVENT);

            JSON_TEXTFIELDTYPES.put(MetadataType.CURRENT_TEMPERATURE);
            JSON_TEXTFIELDTYPES.put(MetadataType.MEDIA_ALBUM);
            JSON_TEXTFIELDTYPES.put(MetadataType.MEDIA_ARTIST);

            JSON_GRID.put(SeatLocation.KEY_GRID, GENERAL_LOCATION_GRID.serializeJSON());
            JSON_SEAT_LOCATIONS.put(JSON_GRID);
            JSON_MODULE_INFO.put(ModuleInfo.KEY_MODULE_ID, TestValues.GENERAL_STRING);
            JSON_MODULE_INFO.put(ModuleInfo.KEY_MODULE_LOCATION, TestValues.JSON_GRID);
            JSON_MODULE_INFO.put(ModuleInfo.KEY_MODULE_SERVICE_AREA, TestValues.JSON_GRID);
            JSON_MODULE_INFO.put(ModuleInfo.KEY_MULTIPLE_ACCESS_ALLOWED, TestValues.GENERAL_BOOLEAN);

            JSON_ROOF_STATUS.put(RoofStatus.KEY_STATUS, GENERAL_DOORSTATUSTYPE);
            JSON_ROOF_STATUS.put(RoofStatus.KEY_LOCATION, JSON_GRID);
            JSON_ROOF_STATUS.put(RoofStatus.KEY_STATE, GENERAL_WINDOW_STATE.serializeJSON());

            JSON_DOOR_STATUS.put(DoorStatus.KEY_STATUS, GENERAL_DOORSTATUSTYPE);
            JSON_DOOR_STATUS.put(DoorStatus.KEY_LOCATION, JSON_GRID);

            JSON_GATE_STATUS.put(GateStatus.KEY_STATUS, GENERAL_DOORSTATUSTYPE);
            JSON_GATE_STATUS.put(GateStatus.KEY_LOCATION, JSON_GRID);

            JSON_ROOF_STATUSES.put(JSON_ROOF_STATUS);
            JSON_DOOR_STATUSES.put(JSON_DOOR_STATUS);
            JSON_GATE_STATUSES.put(JSON_GATE_STATUS);
            JSON_SEEK_STREAMING_INDICATOR.put(SeekStreamingIndicator.KEY_TYPE, SeekIndicatorType.TRACK);

        } catch (JSONException e) {
            Log.e("Test", "Static Json Construction Failed.", e);
        }
    }
}
