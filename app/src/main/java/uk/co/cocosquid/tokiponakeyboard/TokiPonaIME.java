package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.PreferenceManager;


public class TokiPonaIME extends InputMethodService
{
	
	private enum outputMode { LATIN, EMOJI, UNICODE };
	outputMode mode;
	private MyKeyboard keyboard;
	private MyKeyboardEmoji keyboardEmoji;
	private MyKeyboardUnicode keyboardUnicode;
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateInputView()
	{
		updatePreferences();
		
		keyboard = getLayoutInflater().inflate(R.layout.keyboard_wrapper, null).findViewById(R.id.keyboard);
		keyboardEmoji = getLayoutInflater().inflate(R.layout.keyboard_wrapper_emoji, null).findViewById(R.id.keyboard_emoji);
		
		switch (mode)
		{
			case LATIN: return keyboard;
			case EMOJI: return keyboardEmoji;
			case UNICODE: return keyboardUnicode;
			default: return keyboard;
		}
	}
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting)
	{
		super.onStartInput(info, restarting);
		
		InputConnection ic = getCurrentInputConnection();
		InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
		
		updatePreferences();
		
		keyboard.setIMS(this);
		keyboard.loadColors(getBaseContext());
		keyboard.setEditorInfo(info);
		keyboard.setInputConnection(ic);
		keyboard.setIMM(imm);
		keyboard.updateCurrentState();
		
		keyboardEmoji.setIMS(this);
		keyboardEmoji.loadColors(getBaseContext());
		keyboardEmoji.setEditorInfo(info);
		keyboardEmoji.setInputConnection(ic);
		keyboardEmoji.setIMM(imm);
		keyboardEmoji.updateCurrentState();
		
		keyboardUnicode.setIMS(this);
		keyboardUnicode.loadColors(getBaseContext());
		keyboardUnicode.setEditorInfo(info);
		keyboardUnicode.setInputConnection(ic);
		keyboardUnicode.setIMM(imm);
		keyboardUnicode.updateCurrentState();
	}
	
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
	{
		switch (mode)
		{
			case LATIN: keyboard.updateCurrentState(); break;
			case EMOJI: keyboardEmoji.updateCurrentState(); break;
			case UNICODE: keyboardUnicode.updateCurrentState(); break;
			default: keyboard.updateCurrentState();
		}
	}
	
	@Override
	public void onWindowShown()
	{
		super.onWindowShown();
		
		keyboard.loadColors(getBaseContext());
		keyboardEmoji.loadColors(getBaseContext());
	}
	
	@Override
	public void onWindowHidden()
	{
		super.onWindowHidden();
		
		keyboard.finishAction("finish");
		keyboardEmoji.finishAction("finish");
	}
	
	public void cycleMode()
	{
		switch (mode)
		{
			case LATIN:
				mode = outputMode.EMOJI;
				keyboardEmoji.updateCurrentState();
				break;
			case EMOJI:
				mode = outputMode.UNICODE;
				keyboardUnicode.updateCurrentState();
				break;
			case UNICODE:
				mode = outputMode.LATIN;
				keyboard.updateCurrentState();
				break;
		}
	}
	
	private void updatePreferences()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		//boolean previousEmojiMode = emojiMode;
		String outputModeString = sharedPreferences.getString("output_mode", "latin");
		switch (outputModeString)
		{
			case "latin":
				mode = outputMode.LATIN;
				break;
			case "emoji":
				mode = outputMode.EMOJI;
				break;
			case "unicode":
				mode = outputMode.UNICODE;
				break;
			default:
				mode = outputMode.LATIN;
		}
		
		if (keyboard != null)
		{
			switch (mode)
			{
				case LATIN:
					setInputView(keyboard);
					break;
				case EMOJI:
					setInputView(keyboardEmoji);
					break;
				case UNICODE:
					setInputView(keyboardUnicode);
					break;
				default:
					setInputView(keyboard);
			}
		}
	}
}