/**
* Created by AByte0
*/

import java.io.*;
import java.util.Scanner;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;

public class Abyte0_WildernesRunes extends Abyte0_Script
{
	int fMode = 3;
	
	String[] fModeName = {"Attack","Defence","Strength", "Controlled"};
	int[] fModeIdList = {2,3,1,0};
	
	String[] option = {"Yes","No"};
	boolean[] optionList = {true, false};
	
	int airId = 33;
	boolean doAir = true;
	
	int fireId = 31;
	boolean doFire = true;
	
	int waterId = 32;
	boolean doWater = true;
	
	int earthId = 34;
	boolean doEarth = true;
	
	int mindId = 35;
	boolean doMind = true;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"I am buying runes","Magic level?","Are you a strong mage?"};
		return result;
	}

	public Abyte0_WildernesRunes(Extension e)
	{
		super(e);
	}

	public void init(String params)
	{
		print("--");
		print("--");
		print("Abyte0 Mage Area Runes Buyer");
		print("Version 0.0");
		print("--");
		print("--");
		
		
		Frame frame = new Frame("Select Fighting Mode");
		String choiceF = (String)JOptionPane.showInputDialog(frame,		"Fighting Mode:\n", "Fighting Mode Selection",		JOptionPane.PLAIN_MESSAGE, null, fModeName, null);
		for(int i = 0; i < fModeName.length; i++)
		{
			if (fModeName[i].equals(choiceF))
			{
				fMode = fModeIdList[i];
				break;
			}
		}
		print("fMode = " + fMode);
		print("--");
		
	//	Frame frameVials = new Frame("Select Cpt Vials");
	//	String choiceVials = (String)JOptionPane.showInputDialog(frameVials,		"How many Vials:\n", "Vials Cpt Selection",		JOptionPane.PLAIN_MESSAGE, null, cptVialsName, null);
	//	for(int i = 0; i < cptVialsName.length; i++)
	//	{
	//		if (cptVialsName[i].equals(choiceVials))
	//		{
	//			vialsToBuy = cptVialsList[i];
	//			break;
	//		}
	//	}
	//	print("Cpt Vials = " + vialsToBuy);
	//	print("--");
	//	
	//	Frame frameEyes = new Frame("Select Cpt Eyes");
	//	String choiceEyes = (String)JOptionPane.showInputDialog(frameEyes,		"How many Eyes:\n", "Eyes Cpt Selection",		JOptionPane.PLAIN_MESSAGE, null, cptEyesName, null);
	//	for(int i = 0; i < cptEyesName.length; i++)
	//	{
	//		if (cptEyesName[i].equals(choiceEyes))
	//		{
	//			eyesToBuy = cptEyesList[i];
	//			break;
	//		}
	//	}
	//	print("Cpt Eyes = " + eyesToBuy);
	//	print("--");
	}

	public int main()
	{
		SayRandomQuote();
		if(getFightMode() != fMode)
			setFightMode(fMode);
		
		if(isQuestMenu())
		{
			answer(0);
			return 3000;
		}
		if(shopWindowOpen())
		{
			if(doAir && getShopItemIdAmount(airId) > 0)
			{
				buyItemIdFromShop(airId,100);
				return 500;
			}
			
			if(doFire && getShopItemIdAmount(fireId) > 0)
			{
				buyItemIdFromShop(fireId,100);
				return 500;
			}
			
			if(doWater && getShopItemIdAmount(waterId) > 0)
			{
				buyItemIdFromShop(waterId,100);
				return 500;
			}
			
			if(doEarth && getShopItemIdAmount(earthId) > 0)
			{
				buyItemIdFromShop(earthId,100);
				return 500;
			}
			
			if(doMind && getShopItemIdAmount(mindId) > 0)
			{
				buyItemIdFromShop(mindId,100);
				return 500;
			}
			
			return 4000;
		}
		
		int shopNpc[] = getNpcByIdNotTalk(new int[]{793});										
		if (shopNpc[0] != -1)
		{						
			talkToNpc(shopNpc[0]);
			return 4000;
		}
		
		return 5000;
	}
}