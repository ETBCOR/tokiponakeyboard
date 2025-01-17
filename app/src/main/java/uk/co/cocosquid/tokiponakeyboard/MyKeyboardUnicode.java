package uk.co.cocosquid.tokiponakeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

public class MyKeyboardUnicode extends MyKeyboardAbstract
{
	
	// Word construction
	private boolean inBrackets = false;
	private String compoundFirstWordShortcut = "";
	private String suffix = "";
	
	public MyKeyboardUnicode(Context context)
	{
		this(context, null, 0);
	}
	
	public MyKeyboardUnicode(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}
	
	public MyKeyboardUnicode(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context);
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void init(Context context)
	{
		super.init(context);
		
		// Arrays from xml
		Resources res = getResources();
		shortcuts = res.getStringArray(R.array.shortcuts);
		words = res.getStringArray(R.array.words);
		unofficialWords = res.getStringArray(R.array.unofficial_words);
		altWords = res.getStringArray(R.array.alt_words);
	}
	
	protected void action(String startKey, String endKey)
	{
		if (endKey == null)
		{
			
			// Single key sent
			boolean nothingWritten = false;
            /*if (getPreviousCharacter().equals("“") && !getNextCharacter().isEmpty() && !getNextCharacter().equals("”") && !startKey.equals("%“") && !startKey.equals("%delete") && !startKey.equals("%enter")) {
                suffix = " ";
            }*/
			if (startKey.charAt(0) == '%')
			{
				
				// Special key sent
				if (!startKey.equals("%delete"))
				{
					finishAction("finish");
					if (inBrackets && !startKey.equals("%]") && !startKey.equals("%["))
					{
						action("%]", null);
					}
				}
				switch (startKey)
				{
					case "%]":
						
						// Move cursor to the next space (or the end of the input if none are found)
						int endBracketLocation = getEndBracketLocation();
						inputConnection.setSelection(endBracketLocation, endBracketLocation);
						
						// Place a closing bracket if it is missing
						if (currentText.charAt(endBracketLocation - 1) != ']')
						{
							write("]");
						}
						
						setBracket(false);
						break;
					case "%[":
						if (inBrackets)
						{
							action("%]", null);
						}
						else
						{
							writeShortcut("[%");
							
							// Move cursor inside brackets
							moveCursorBackOne();
							
							setBracket(true);
						}
						break;
                    /*case "%\"":
                        /* if (quoteNestingLevel > 0) {
                            write("”");
                            if (!",.:?!\n".contains(getNextCharacter()) && !getNextCharacter().isEmpty()) {
                                write(" ");
                            }
                        } else {
                            writeShortcut("“%");
                            if (getNextCharacter().equals(" ")) {
                                inputConnection.deleteSurroundingText(0, 1);
                            }
                        }
                        break;
                    */
					case "%\"":
					case "%.":
					case "%?":
						write(Character.toString(startKey.charAt(1)));
						//action(startKey.charAt(1) + "%", null);
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
					currentShortcut = "";
					setLayout("");
					
				}
				else if (doesShortcutExist(finishShortcut(currentShortcut + startKey)))
				{
					
					// Key is part of previous action but it is still unfinished
					currentShortcut += startKey;
					setLayout("");
					setLayout(currentShortcut);
					nothingWritten = true;
					
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
			if (!suffix.isEmpty() && !nothingWritten)
			{
				moveCursorBackOne();
				suffix = "";
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
					switch (startKey)
					{
						case "%[":
							write(",");
							break;
						case "%\"":
							// If previous word has a varient, cycle through it
							break;
						case "%.":
							write(":");
							break;
						case "%?":
							write("!");
							break;
						case "%enter":
							inputMethodManager.showInputMethodPicker();
							break;
						default:
							Log.e(null, "Shortcut: " + startKey + " is not a special key");
					}
				}
				else if (startKey.charAt(0) == '%')
				{
					
					// Special key followed by normal key
					if (startKey.equals("%enter"))
					{
						
						// Switch subtype
						finishAction("finish");
						if ("aeijklmnopstuwAEIJKLMNOPSTUW".contains(getPreviousCharacter()))
						{
							write(" ");
						}
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
				compoundFirstWordShortcut = finishShortcut(currentShortcut + startKey);
				currentShortcut = "";
				action(endKey, null);
			}
		}
	}
	
	// Returns true if the cursor is at the start of an input, newline or quote
	private boolean cursorAtStart()
	{
		updateTextInfo();
		if (beforeCursorText.length() == 0)
		{
			return true;
		}
		else
		{
			String previousCharacter = getPreviousCharacter();
			return "\n“".contains(previousCharacter);
		}
	}
	
	protected void delete()
	{
		if (currentShortcut.isEmpty())
		{
			
			// Delete some text
			updateTextInfo();
			label:
			for (int i = beforeCursorText.length() - 1; i >= 0; i--)
			{
				String currentString = Character.toString(beforeCursorText.charAt(i));
				switch (currentString)
				{
					case "\n":
					case "“":
						if (i == beforeCursorText.length() - 1)
						{
							inputConnection.deleteSurroundingText(1, 0);
						}
						else
						{
							inputConnection.deleteSurroundingText(beforeCursorText.length() - i - 1, 0);
						}
						break label;
					case " ":
					case "_":
					case ",":
					case "”":
					case ".":
					case ":":
					case "?":
					case "!":
						inputConnection.deleteSurroundingText(beforeCursorText.length() - i, 0);
						break label;
					case "]":
						
						// Move inside the brackets and delete from there
						inputConnection.setSelection(i, i);
						setBracket(true);
						delete();
						break label;
					
					case "[":
						
						// Delete everything from the opening bracket up to the closing bracket
						int endBracket = getEndBracketLocation();
						inputConnection.deleteSurroundingText(1, endBracket - beforeCursorText.length());
						//if (getNextCharacter().equals(" ")) {
						//    inputConnection.deleteSurroundingText(0, 1);
						//}
						setBracket(false);
						break label;
					//return;
				}
				if (i == 0)
				{
					
					// Start of the input was reached
					inputConnection.deleteSurroundingText(beforeCursorText.length(), 0);
				}
			}
			
			if ("% |“ ".contains(getAdjacentCharacters()))
			{
				inputConnection.deleteSurroundingText(0, 1);
			}
			else if (" %|  | ,| ”| .| :| ?| !| \n".contains(getAdjacentCharacters()))
			{
				inputConnection.deleteSurroundingText(1, 0);
			}
		}
		else
		{
			
			// Cancel current input in progress
			currentShortcut = "";
			compoundFirstWordShortcut = "";
			setLayout("");
		}
	}
	
	/* nextKey allows the function to know whether or not to reset currentShortcut in the cases
	 * where a new compound glyph has been started.
	 */
	public void finishAction(String nextKey)
	{
		boolean validCombination = doesShortcutExist(finishShortcut(currentShortcut + nextKey));
		if (!validCombination || !compoundFirstWordShortcut.isEmpty())
		{
			writeShortcut(finishShortcut(currentShortcut));
			if (!validCombination)
			{
				currentShortcut = "";
			}
			setLayout("");
		}
	}
	
	private int getEndBracketLocation()
	{
		updateTextInfo();
		int endBracket = beforeCursorText.length() - 1;
		while (true)
		{
			if (currentText.charAt(endBracket) == ']' || endBracket == currentText.length() - 1)
			{
				
				// A bracket was found or the end of the input was reached
				return endBracket + 1;
				
			}
			endBracket++;
		}
	}
	
	public void setBracket(boolean newInBrackets)
	{
		inBrackets = newInBrackets;
		if (inBrackets)
		{
			((Button) findViewById(R.id.btn22)).setText("]");
		}
		else
		{
			((Button) findViewById(R.id.btn22)).setText("[");
		}
	}
	
	public void updateCurrentState()
	{
		
		// Get the adjacent characters
		String charOnRight = getNextCharacter();
		String charOnLeft = getPreviousCharacter();
		
		boolean adjust = true;
		if ("],”.:?!\n".contains(charOnLeft) || " _],”.:?!\n".contains(charOnRight))
		{
			
			// Do not adjust cursor position
			adjust = false;
		}
		
		int moveTo = 0;
		int i;
		label:
		for (i = beforeCursorText.length() - 1; i >= 0; i--)
		{
			String currentString = Character.toString(beforeCursorText.charAt(i));
			switch (currentString)
			{
				case "“":
					if (moveTo == 0)
					{
						moveTo = i + 1;
					}
					break;
				case "\n":
					if (moveTo == 0)
					{
						moveTo = i + 1;
					}
					setBracket(false);
					break label;
				case " ":
				case "]":
					setBracket(false);
					break label;
				case "_":
				case "[":
					setBracket(true);
					break label;
			}
			if (i == 0)
			{
				setBracket(false);
				break;
			}
		}
		if (adjust)
		{
			if (moveTo == 0)
			{
				moveTo = i;
			}
			inputConnection.setSelection(moveTo, moveTo);
		}
		
		// Ensure the correct quote is on the key
        /* updateQuoteNestedLevel();
        if (quoteNestingLevel > 0)
        {
            ((Button) findViewById(R.id.quote)).setText("”");
        }
        else
        {
            ((Button) findViewById(R.id.quote)).setText("“");
        }
        */
	}
	
	private void write(String toWrite)
	{
		if (inBrackets && ",:!".contains(toWrite))
		{
			action("%]", null);
		}
		inputConnection.commitText(toWrite + suffix, 1);
	}
	
	private void writeShortcut(String shortcut)
	{
		
		// Do not write anything if the shortcut is empty
		if (shortcut.isEmpty())
		{
			return;
		}
		
		// Decide the correct word spacer to put before the word
		String wordSpacer = " ";
		if (cursorAtStart())
		{
			wordSpacer = "";
		}
		else if (inBrackets)
		{
			wordSpacer = "_";
		}
		
		// Prepare first part of a compound glyph if it exists
		String compoundFirstWord = "";
		if (!compoundFirstWordShortcut.isEmpty())
		{
			compoundFirstWord = getWord(compoundFirstWordShortcut) + "-";
			compoundFirstWordShortcut = "";
		}
		
		write(wordSpacer + compoundFirstWord + getWord(shortcut));
	}
}