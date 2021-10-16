package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import java.io.IOError;
import java.io.IOException;

public abstract class MyKeyboardAbstract extends LinearLayout implements View.OnLongClickListener, View.OnClickListener
{
	
	// Input connection
	protected InputConnection inputConnection;
	protected EditorInfo editorInfo;
	protected InputMethodManager inputMethodManager;
	protected TokiPonaIME inputMethodService;
	
	// Key detection
	protected int previousKey;
	protected int currentKey;
	protected int startKey;
	protected boolean actionComplete = false;
	
	// Arrays
	protected SparseArray<String> keyValues = new SparseArray<>();
	protected String[] shortcuts;
	protected String[] words;
	protected String[] unofficialWords;
	protected String[] altWords;
	
	// Word construction
	protected int quoteNestingLevel = 0;
	protected String currentShortcut = "";
	
	// Text manipulation
	protected CharSequence currentText;
	protected CharSequence beforeCursorText;
	protected CharSequence afterCursorText;
	protected boolean stopDelete = false;
	
	// Preferences
	SharedPreferences sharedPreferences;
	
	// Colours
	protected int[] colours = new int[19];
	
	protected Button[] keys = new Button[28];
	
	public MyKeyboardAbstract(Context context)
	{
		this(context, null, 0);
	}
	
	public MyKeyboardAbstract(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}
	
	public MyKeyboardAbstract(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}
	
	@Override
	public void onClick(View v)
	{
		action(keyValues.get(v.getId()), null);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event)
	{
		
		// Find what key the event is on
		boolean found = false;
		boolean keyChanged = false;
		final Rect childRect = new Rect();
		final Rect parentRect = new Rect();
		
		for (int i = 0; i < keys.length; i++)
		{
			Button key = keys[i];
			key.getHitRect(childRect);
			((View) key.getParent()).getHitRect(parentRect);
			if (event.getX() > childRect.left && event.getX() < childRect.right && event.getY() > parentRect.top && event.getY() < parentRect.bottom)
			{
				if (i != currentKey)
				{
					previousKey = currentKey;
					currentKey = i;
					keyChanged = true;
				}
				found = true;
				break;
			}
		}
		
		// Highlight key under finger
		if (keyChanged)
		{
			keys[previousKey].setPressed(false);
		}
		keys[currentKey].setPressed(found);
		
		if (found)
		{
			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				actionComplete = false;
				startKey = currentKey;
			}
			else if (event.getAction() == MotionEvent.ACTION_MOVE)
			{
				if (currentKey != startKey)
				{
					setLayout("");
					stopDelete = true;
				}
				else if (!actionComplete)
				{
					setLayout("");
				}
			}
			else if (event.getAction() == MotionEvent.ACTION_UP && currentKey != startKey)
			{
				
				// Swipe complete
				if (actionComplete)
				{
					currentShortcut = "";
					action(keyValues.get(keys[currentKey].getId()), null);
				}
				else
				{
					actionComplete = true;
					action(keyValues.get(keys[startKey].getId()), keyValues.get(keys[currentKey].getId()));
				}
				keys[currentKey].setPressed(false);
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean onLongClick(View v)
	{
		action(keyValues.get(v.getId()), keyValues.get(v.getId()));
		actionComplete = true;
		return true;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	protected void setDeleteListener(Button delete)
	{
		
		// Hold delete key for continuous delete
		delete.setOnTouchListener(new View.OnTouchListener()
		{
			
			private Handler mHandler;
			
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if (mHandler != null)
					{
						return true;
					}
					stopDelete = false;
					mHandler = new Handler();
					mHandler.postDelayed(mAction, 200);
				}
				else if (event.getAction() == MotionEvent.ACTION_UP || stopDelete)
				{
					if (mHandler == null)
					{
						return true;
					}
					mHandler.removeCallbacks(mAction);
					mHandler = null;
				}
				return false;
			}
			
			Runnable mAction = new Runnable()
			{
				@Override
				public void run()
				{
					delete();
					mHandler.postDelayed(this, 30);
				}
			};
			
		});
	}
	
	protected void action(String startKey, String endKey)
	{
	}
	
	protected void delete()
	{
	}
	
	protected boolean doesShortcutExist(String shortcutToCheck)
	{
		for (String shortcut : shortcuts)
		{
			if (shortcutToCheck.equals(shortcut))
			{
				return true;
			}
		}
		return false;
	}
	
	protected void enter()
	{
		
		// Send the correct IME action specified by the editor
		switch (editorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION))
		{
			case EditorInfo.IME_ACTION_GO:
				inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO);
				break;
			case EditorInfo.IME_ACTION_NEXT:
				inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT);
				break;
			case EditorInfo.IME_ACTION_SEARCH:
				inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
				break;
			case EditorInfo.IME_ACTION_SEND:
				inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND);
				break;
			default:
				inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
				break;
		}
	}
	
	// Returns a shortcut if it exists and returns the finished shortcut if it does not exist yet
	protected String finishShortcut(String shortcutToFinish)
	{
		if (doesShortcutExist(shortcutToFinish) || shortcutToFinish.isEmpty())
		{
			return shortcutToFinish;
		}
		else
		{
			return shortcutToFinish + shortcutToFinish.charAt(shortcutToFinish.length() - 1);
		}
	}
	
	protected String getAdjacentCharacters()
	{
		updateCurrentState();
		String previous = getPreviousCharacter();
		if (previous.isEmpty())
		{
			previous = "%";
		}
		String next = getNextCharacter();
		if (next.isEmpty())
		{
			next = "%";
		}
		return previous + next;
	}
	
	protected String getNextCharacter()
	{
		updateTextInfo();
		boolean atEndOfInput = afterCursorText.length() == 0;
		String charOnRight = "";
		if (!atEndOfInput)
		{
			charOnRight = Character.toString(afterCursorText.charAt(0));
		}
		return charOnRight;
	}
	
	protected String getPreviousCharacter()
	{
		updateTextInfo();
		boolean atStartOfInput = beforeCursorText.length() == 0;
		String charOnLeft = "";
		if (!atStartOfInput)
		{
			charOnLeft = Character.toString(beforeCursorText.charAt(beforeCursorText.length() - 1));
		}
		return charOnLeft;
	}
	
	protected String getWord(String shortcut)
	{
		for (int i = 0; i < shortcuts.length; i++)
		{
			if (shortcut.equals(shortcuts[i]))
			{
				return words[i];
			}
		}
		return "";
	}
	
	protected boolean isUnofficial(String word)
	{
		for (String unofficialWord : unofficialWords)
		{
			if (unofficialWord.equals(word))
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean isAlt(String word)
	{
		for (String altWord : altWords)
		{
			if (altWord.equals(word))
			{
				return true;
			}
		}
		return false;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	protected void init(Context context)
	{
		// Load shared preferences
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		LayoutInflater.from(context).inflate(R.layout.keyboard, this, true);
		
		// Setup keys and load colors
		setKeys();
		loadColors(context);
	}
	
	public void loadColors(Context context)
	{
		
		// Load colors
		TypedArray theme_default = context.getResources().obtainTypedArray(R.array.theme_default);
		TypedArray theme_light = context.getResources().obtainTypedArray(R.array.theme_light);
		TypedArray theme_dark = context.getResources().obtainTypedArray(R.array.theme_dark);
		
		TypedArray selected_theme;
		
		switch (sharedPreferences.getString("themes", "default"))
		{
			case "light":
				selected_theme = theme_light;
				break;
			case "dark":
				selected_theme = theme_dark;
				break;
			default:
				selected_theme = theme_default;
				break;
		}
		
		for (int i = 0; i < 19; i++)
			colours[i] = selected_theme.getColor(i, 0);
		
		setColours();
	}
	
	public void setColours()
	{
		for (int i = 0; i < keys.length; i++)
		{
			// Set base colours
			if (i < 14)
			{
				
				// Letter keys
				try
				{
					keys[i].setBackgroundTintList(ColorStateList.valueOf(this.colours[0]));
					keys[i].setTextColor(this.colours[1]);
				} catch (Exception e)
				{
					e.printStackTrace();
					return;
				}
				
			}
			else if (i < 22)
			{
				
				// Common word keys
				try
				{
					keys[i].setBackgroundTintList(ColorStateList.valueOf(colours[2]));
					keys[i].setTextColor(this.colours[3]);
				} catch (Exception e)
				{
					e.printStackTrace();
					return;
				}
				
			}
			else
			{
				
				// Special keys
				try
				{
					keys[i].setBackgroundTintList(ColorStateList.valueOf(colours[4]));
					keys[i].setTextColor(this.colours[5]);
				} catch (Exception e)
				{
					e.printStackTrace();
					return;
				}
			}
		}
		
		// Set background colour
		try
		{
			findViewById(R.id.keyboard).setBackgroundColor(this.colours[18]);
		} catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	protected void moveCursorBackOne()
	{
		updateTextInfo();
		int backOne = beforeCursorText.length() - 1;
		inputConnection.setSelection(backOne, backOne);
	}
	
	public void setKeys()
	{
		// Set the keys
		keys[0] = findViewById(R.id.ali);
		keys[1] = findViewById(R.id.en);
		keys[2] = findViewById(R.id.ike);
		keys[3] = findViewById(R.id.jan);
		keys[4] = findViewById(R.id.kama);
		keys[5] = findViewById(R.id.la);
		keys[6] = findViewById(R.id.ma);
		keys[7] = findViewById(R.id.nimi);
		keys[8] = findViewById(R.id.o);
		keys[9] = findViewById(R.id.pi);
		keys[10] = findViewById(R.id.sina);
		keys[11] = findViewById(R.id.tawa);
		keys[12] = findViewById(R.id.utala);
		keys[13] = findViewById(R.id.wile);
		
		keys[14] = findViewById(R.id.a);
		keys[15] = findViewById(R.id.ala);
		keys[16] = findViewById(R.id.e);
		keys[17] = findViewById(R.id.li);
		keys[18] = findViewById(R.id.mi);
		keys[19] = findViewById(R.id.ni);
		keys[20] = findViewById(R.id.pona);
		keys[21] = findViewById(R.id.toki);
		
		keys[22] = findViewById(R.id.bracket);
		keys[23] = findViewById(R.id.quote);
		keys[24] = findViewById(R.id.dot);
		keys[25] = findViewById(R.id.question);
		keys[26] = findViewById(R.id.enter);
		keys[27] = findViewById(R.id.delete);
		
		setDeleteListener(keys[27]);
		
		// Set key listeners
		for (Button key : keys)
		{
			key.setOnClickListener(this);
			key.setOnLongClickListener(this);
			key.setTextSize(18);
		}
		
		// Set the button strings
		keyValues.put(R.id.ali, "a");
		keyValues.put(R.id.en, "e");
		keyValues.put(R.id.ike, "i");
		keyValues.put(R.id.jan, "j");
		keyValues.put(R.id.kama, "k");
		keyValues.put(R.id.la, "l");
		keyValues.put(R.id.ma, "m");
		keyValues.put(R.id.nimi, "n");
		keyValues.put(R.id.o, "o");
		keyValues.put(R.id.pi, "p");
		keyValues.put(R.id.sina, "s");
		keyValues.put(R.id.tawa, "t");
		keyValues.put(R.id.utala, "u");
		keyValues.put(R.id.wile, "w");
		
		keyValues.put(R.id.a, "a%");
		keyValues.put(R.id.ala, "ala%");
		keyValues.put(R.id.e, "e%");
		keyValues.put(R.id.li, "li%");
		keyValues.put(R.id.mi, "mi%");
		keyValues.put(R.id.ni, "ni%");
		keyValues.put(R.id.pona, "pona%");
		keyValues.put(R.id.toki, "toki%");
		
		keyValues.put(R.id.bracket, "%[");
		keyValues.put(R.id.dot, "%.");
		keyValues.put(R.id.quote, "%\"");
		keyValues.put(R.id.question, "%?");
		keyValues.put(R.id.enter, "%enter");
		
		keyValues.put(R.id.delete, "%delete");
	}
	
	public void setEditorInfo(EditorInfo ei)
	{
		editorInfo = ei;
	}
	
	public void setIMM(InputMethodManager imm)
	{
		inputMethodManager = imm;
	}
	
	public void setIMS(TokiPonaIME ims)
	{
		inputMethodService = ims;
	}
	
	public void setInputConnection(InputConnection ic)
	{
		inputConnection = ic;
	}
	
	protected void setLayout(String layoutShortcut)
	{
		String letters = "aeijklmnopstuw";
		for (int i = 0; i < 14; i++)
		{
			String potentialShortcut = layoutShortcut + letters.charAt(i);
			Button key = keys[i];
			String keyText;
			if (doesShortcutExist(potentialShortcut))
			{
				
				// Key on last state
				keyText = getWord(potentialShortcut);
				key.setText(keyText);
				
				// Set the colours
				if (isUnofficial(keyText))
				{
					key.setBackgroundTintList(ColorStateList.valueOf(this.colours[14]));
					key.setTextColor(this.colours[15]);
				}
				else if (isAlt(keyText))
				{
					key.setBackgroundTintList(ColorStateList.valueOf(this.colours[10]));
					key.setTextColor(this.colours[11]);
				}
				else
				{
					key.setBackgroundTintList(ColorStateList.valueOf(this.colours[6]));
					key.setTextColor(this.colours[7]);
				}
				
			}
			else if (doesShortcutExist(finishShortcut(potentialShortcut)))
			{
				
				keyText = getWord(finishShortcut(potentialShortcut));
				key.setText(keyText);
				if (potentialShortcut.length() > 1)
				{
					
					// Key is on intermediate state
					// Set the colours
					if (isUnofficial(keyText))
					{
						key.setBackgroundTintList(ColorStateList.valueOf(this.colours[16]));
						key.setTextColor(this.colours[17]);
					}
					else if (isAlt(keyText))
					{
						key.setBackgroundTintList(ColorStateList.valueOf(this.colours[12]));
						key.setTextColor(this.colours[13]);
					}
					else
					{
						key.setBackgroundTintList(ColorStateList.valueOf(this.colours[8]));
						key.setTextColor(this.colours[9]);
					}
				}
				else
				{
					
					// Key is on base state
					// Set the colours
					key.setBackgroundTintList(ColorStateList.valueOf(this.colours[0]));
					key.setTextColor(this.colours[1]);
				}
			}
		}
	}
	
	public void updateCurrentState()
	{
	}
	
	protected void updateQuoteNestedLevel()
	{
		updateTextInfo();
		quoteNestingLevel = 0;
		for (int i = beforeCursorText.length() - 1; i >= 0; i--)
		{
			if (beforeCursorText.charAt(i) == '“')
			{
				quoteNestingLevel += 1;
			}
			else if (beforeCursorText.charAt(i) == '”')
			{
				quoteNestingLevel -= 1;
			}
		}
	}
	
	protected void updateTextInfo()
	{
		ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if (extractedText != null)
		{
			currentText = extractedText.text;
			beforeCursorText = inputConnection.getTextBeforeCursor(currentText.length(), 0);
			afterCursorText = inputConnection.getTextAfterCursor(currentText.length(), 0);
		}
		else
		{
			currentText = beforeCursorText = afterCursorText = "";
		}
	}
}