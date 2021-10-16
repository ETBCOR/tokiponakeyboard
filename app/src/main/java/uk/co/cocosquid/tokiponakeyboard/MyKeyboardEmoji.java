package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.preference.PreferenceManager;

public class MyKeyboardEmoji extends MyKeyboardAbstract
{
	
	public MyKeyboardEmoji(Context context)
	{
		this(context, null, 0);
	}
	
	public MyKeyboardEmoji(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}
	
	public MyKeyboardEmoji(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context);
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
				if (currentKey != startKey || keyValues.get(keys[startKey].getId()).equals("%shift"))
				{
					if (currentShortcut.equals("X") || currentShortcut.equals("x"))
					{
						setLayout("x");
					}
					else if (keyValues.get(keys[startKey].getId()).equals("%shift"))
					{
						setLayout("X");
					}
					else
					{
						setLayout("");
					}
					stopDelete = true;
				}
				else if (!actionComplete)
				{
					if (currentShortcut.equals("X") || currentShortcut.equals("x"))
					{
						setLayout("x");
					}
					else if (keyValues.get(keys[startKey].getId()).equals("%shift"))
					{
						setLayout("X");
					}
					else
					{
						setLayout("");
					}
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
	
	@SuppressLint("ClickableViewAccessibility")
	protected void init(Context context)
	{
		super.init(context);
		
		// Arrays from xml
		Resources res = getResources();
		shortcuts = res.getStringArray(R.array.shortcuts_emoji);
		words = res.getStringArray(R.array.words_emoji);
		unofficialWords = res.getStringArray(R.array.unofficial_words_emoji);
		altWords = res.getStringArray(R.array.alt_words);
	}
	
	protected void action(String startKey, String endKey)
	{
		if (startKey == null) return;
		else if (endKey == null)
		{
			// Single key sent
			
			if (startKey.charAt(0) == '%')
			{
				
				// Special key sent
				if (!startKey.equals("%delete") && !currentShortcut.equals("X") && !currentShortcut.equals("x"))
				{
					finishAction("finish");
				}
				
				switch (startKey)
				{
					case "%shift":
						
						// Latin input
						if (currentShortcut.equals("X") || currentShortcut.equals("x"))
						{
							currentShortcut = "";
						}
						else
						{
							currentShortcut = "X";
						}
						setLayout(currentShortcut);
						
						break;
					case "%delete":
						delete();
						break;
					case "%enter":
						enter();
						break;
					default:
						Log.e(null, "Shortcut: " + startKey + " is not a special key");
				}
				
			}
			else
			{
				
				// Letter/word key sent
				if (doesShortcutExist(currentShortcut + startKey))
				{
					
					// Key is part of previous action and it is now finished
					writeShortcut(currentShortcut + startKey);
					if (currentShortcut.equals("X") || currentShortcut.equals("x"))
					{
						currentShortcut = "x";
					}
					else
					{
						currentShortcut = "";
					}
					setLayout(currentShortcut);
					
				}
				else if (doesShortcutExist(finishShortcut(currentShortcut + startKey)))
				{
					
					// Key is part of previous action but it is still unfinished
					currentShortcut += startKey;
					setLayout("");
					setLayout(currentShortcut);
					
				}
				else
				{
					
					// Need to finish previous action
					finishAction("finish");
					if (doesShortcutExist(startKey))
					{
						
						// Word key sent
						writeShortcut(startKey);
						
					}
					else
					{
						
						// Letter key sent
						currentShortcut = startKey;
						setLayout(currentShortcut);
					}
				}
			}
		}
		else
		{
			
			// Two keys sent
			if (startKey.charAt(0) == '%' || endKey.charAt(0) == '%')
			{
				
				// Two separate keys to be sent (at least one was a special key)
				if (startKey.equals(endKey))
				{
					
					// Long press actions for special keys
					finishAction("finish");
					if (startKey.equals("%enter"))
					{
						inputMethodManager.showInputMethodPicker();
					}
					else
					{
						Log.e(null, "Shortcut: " + startKey + " is not a special key");
					}
					
					// Special key followed by normal key
				}
				else if (startKey.charAt(0) == '%')
				{
					
					if (startKey.equals("%enter"))
					{
						
						// Switch subtype
						finishAction("finish");
						inputMethodService.cycleMode();
						
					}
					else
					{
						action(startKey, null);
						action(endKey, null);
					}
					
				}
				else
				{
					
					// Normal key followed by special key
					action(startKey, null);
					finishAction("finish");
					action(endKey, null);
				}
			}
			else
			{
				
				// A compound glyph to be sent (Both were letter/word keys)
				finishAction(startKey);
				writeShortcut(finishShortcut(currentShortcut + startKey));
				currentShortcut = "";
				action(endKey, null);
			}
		}
	}
	
	protected void delete()
	{
		if ("Xx".contains(currentShortcut))
		{
			inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			updateCurrentState();
		}
		else
		{
			
			// Cancel current input in progress
			currentShortcut = "";
			setLayout("");
		}
	}
	
	/* nextKey allows the function to know whether or not to reset currentShortcut in the cases
	 * where a new compound glyph has been started.
	 */
	public void finishAction(String nextKey)
	{
		boolean validCombination = doesShortcutExist(finishShortcut(currentShortcut + nextKey));
		if (!validCombination)
		{
			writeShortcut(finishShortcut(currentShortcut));
			currentShortcut = "";
			setLayout("");
		}
	}
	
	public void updateCurrentState()
	{
		
		// Get the adjacent characters
		String charOnLeft = getPreviousCharacter();
		if ("aeijklmnopstuwAEIJKLMNOPSTUW".contains(charOnLeft) && !charOnLeft.isEmpty())
		{
			currentShortcut = "x";
		}
		else if (currentShortcut.equals("x"))
		{
			currentShortcut = "";
		}
		setLayout(currentShortcut);
	}
	
	private void write(String toWrite)
	{
		inputConnection.commitText(toWrite, 1);
	}
	
	private void writeShortcut(String shortcut)
	{
		
		// Do not write anything if the shortcut is empty
		if (shortcut.isEmpty())
		{
			return;
		}
		
		write(getWord(shortcut));
	}
}