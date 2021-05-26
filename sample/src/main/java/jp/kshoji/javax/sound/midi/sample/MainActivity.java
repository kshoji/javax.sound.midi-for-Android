package jp.kshoji.javax.sound.midi.sample;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.kshoji.blemidi.util.BleUtils;
import jp.kshoji.javax.sound.midi.BleMidiSystem;
import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.UsbMidiSystem;
import jp.kshoji.javax.sound.midi.impl.SequencerImpl;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileWriter;
import jp.kshoji.javax.sound.midi.spi.MidiFileReader;
import jp.kshoji.javax.sound.midi.spi.MidiFileWriter;

public class MainActivity extends ActionBarActivity {
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2847;
    private static final int REQUEST_CODE_MIDI_FILE_CHOOSER = 2848;
    private static final String TAG = "MainActivity";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss.SSS", Locale.US);

    SequencerImpl sequencer = new SequencerImpl();
    UsbMidiSystem usbMidiSystem;
    BleMidiSystem bleMidiSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        findViewById(R.id.button1).setOnClickListener(v -> {
            MidiDevice.Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo();
            Toast.makeText(this, "" + midiDeviceInfo.length + " device" + (midiDeviceInfo.length == 1 ? "" : "s")  +" connected", Toast.LENGTH_LONG).show();

            for (MidiDevice.Info info : midiDeviceInfo) {
                try {
                    MidiDevice midiDevice = MidiSystem.getMidiDevice(info);

                    Log.i(TAG, "deviceName: " + midiDevice.getDeviceInfo().getName());
                } catch (MidiUnavailableException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        final Spinner spinner = findViewById(R.id.divisionType);
        spinner.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, new String[]{
                "PPQ",
                "SMPTE 24",
                "SMPTE 25",
                "SMPTE 30",
                "SMPTE 30 DROP",
        }));

        findViewById(R.id.recordButton).setOnClickListener(v -> {
            try {
                // Configure division type, and resolution
                float divisionType;
                switch (spinner.getSelectedItemPosition()) {
                    case 0:
                        divisionType = Sequence.PPQ;
                        break;
                    case 1:
                        divisionType = Sequence.SMPTE_24;
                        break;
                    case 2:
                        divisionType = Sequence.SMPTE_25;
                        break;
                    case 3:
                        divisionType = Sequence.SMPTE_30;
                        break;
                    case 4:
                        divisionType = Sequence.SMPTE_30DROP;
                        break;

                    case Spinner.INVALID_POSITION:
                    default:
                        divisionType = Sequence.PPQ;
                        break;
                }

                EditText resolution = findViewById(R.id.resolution);

                // Set a new Sequence for Sequencer
                sequencer.setSequence(new Sequence(divisionType, Integer.parseInt(resolution.getText().toString()), 1));
            } catch (InvalidMidiDataException e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            // Open the Sequencer
            if (!sequencer.isOpen()) {
                try {
                    sequencer.open();
                } catch (MidiUnavailableException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            // Start recording
            sequencer.startRecording();
        });

        final SeekBar sequencePosition = findViewById(R.id.sequencePosition);
        sequencePosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (sequencer != null) {
                    Log.i(TAG, "setTickPosition: " + sequencePosition.getProgress());
                    sequencer.setTickPosition(sequencePosition.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sequencePosition.setEnabled(false);

        final SeekBar tempoFactor = findViewById(R.id.tempoFactor);
        tempoFactor.setMax(12);
        tempoFactor.setProgress(4);
        tempoFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 0.5 to 2.0 (min 0, max 12) 0.5 + progress * 0.125
                sequencer.setTempoFactor(0.5f + seekBar.getProgress() * 0.125f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.tempoFactorReset).setOnClickListener(v -> tempoFactor.setProgress(4));

        findViewById(R.id.playButton).setOnClickListener(v -> {
            sequencer.start();
        });

        findViewById(R.id.stopButton).setOnClickListener(v -> {
            sequencer.stop();
            updateSequenceInfo();
        });

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            Sequence sequence = sequencer.getSequence();
            if (sequence == null || sequence.getTickLength() == 0) {
                Log.i(TAG, "nothing to save");
                return;
            }

            MidiFileWriter midiFileWriter = new StandardMidiFileWriter();
            try {
                String filename = dateFormat.format(new Date()) + ".mid";
                midiFileWriter.write(sequence, 0, new File(getExternalFilesDir(null), filename));
                Toast.makeText(this, filename + " saved.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });

        findViewById(R.id.loadButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/midi");
            Intent chooserIntent = Intent.createChooser(intent, "MIDI File");
            startActivityForResult(chooserIntent, REQUEST_CODE_MIDI_FILE_CHOOSER);
        });

        usbMidiSystem = new UsbMidiSystem(this);
        usbMidiSystem.initialize();
        Log.d(TAG, "UsbMidiSystem initialized");

        // check Permissions for Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale("android.permission.ACCESS_FINE_LOCATION")) {
                new AlertDialog.Builder(this)
                        .setMessage("Please accept Location Permission to enable BLE MIDI functions.")
                        .setPositiveButton("OK", (dialog1, which) -> requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_CODE_PERMISSION_LOCATION))
                        .create()
                        .show();
                return;
            } else {
                requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_CODE_PERMISSION_LOCATION);
                return;
            }
        }

        if (!BleUtils.isBluetoothEnabled(this)) {
            BleUtils.enableBluetooth(this);
            return;
        }
        bleMidiSystem = new BleMidiSystem(this);
        bleMidiSystem.initialize();
        bleMidiSystem.startScanDevice();
        Log.d(TAG, "BleMidiSystem initialized");
    }

    private void updateSequenceInfo() {
        SeekBar sequencePosition = findViewById(R.id.sequencePosition);
        Sequence sequence = sequencer.getSequence();
        TextView sequenceLength = findViewById(R.id.sequenceLength);
        sequencePosition.setEnabled(false);
        if (sequence == null) {
            sequenceLength.setText("Sequence length: 0, ticks: 0");
            return;
        }

        Track[] tracks = sequence.getTracks();
        int trackSize = 0;
        long trackTicks = 0;
        for (Track track: tracks) {
            trackSize = Math.max(trackSize, track.size());
            trackTicks = Math.max(trackTicks, track.ticks());
        }
        if (trackTicks > 0) {
            sequencePosition.setMax((int) trackTicks);
            sequencePosition.setEnabled(true);
        }
        sequenceLength.setText("Sequence length: " + trackSize + ", ticks: " + trackTicks);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_MIDI_FILE_CHOOSER) {
            if (data == null) {
                return;
            }
            MidiFileReader midiFileReader = new StandardMidiFileReader();
            try (InputStream inputStream = getContentResolver().openInputStream(data.getData())) {
                sequencer.setSequence(midiFileReader.getSequence(inputStream));
                updateSequenceInfo();
                Toast.makeText(this, data.getDataString() + " loaded.", Toast.LENGTH_LONG).show();
            } catch (InvalidMidiDataException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return;
        } else if (requestCode == BleUtils.REQUEST_CODE_BLUETOOTH_ENABLE) {
            if (!BleUtils.isBluetoothEnabled(this)) {
                return;
            }

            bleMidiSystem = new BleMidiSystem(this);
            bleMidiSystem.initialize();
            bleMidiSystem.startScanDevice();
            Log.d(TAG, "BleMidiSystem initialized");
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!BleUtils.isBluetoothEnabled(this)) {
                    BleUtils.enableBluetooth(this);
                    return;
                }

                bleMidiSystem = new BleMidiSystem(this);
                bleMidiSystem.initialize();
                bleMidiSystem.startScanDevice();
                Log.d(TAG, "BleMidiSystem initialized");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        if (usbMidiSystem != null) {
            usbMidiSystem.terminate();
        }
        if (bleMidiSystem != null) {
            bleMidiSystem.terminate();
        }
        super.onDestroy();
    }
}
