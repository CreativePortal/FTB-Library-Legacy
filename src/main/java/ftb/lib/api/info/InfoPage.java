package ftb.lib.api.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import ftb.lib.JsonHelper;
import ftb.lib.api.gui.widgets.ButtonLM;
import ftb.lib.api.info.lines.InfoExtendedTextLine;
import ftb.lib.api.info.lines.InfoTextLine;
import ftb.lib.mod.client.gui.info.ButtonInfoPage;
import ftb.lib.mod.client.gui.info.GuiInfo;
import ftb.lib.mod.net.MessageDisplayInfo;
import latmod.lib.LMJsonUtils;
import latmod.lib.LMMapUtils;
import latmod.lib.RemoveFilter;
import latmod.lib.util.FinalIDObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IJsonSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfoPage extends FinalIDObject implements IJsonSerializable // GuideFile
{
	private static final RemoveFilter<Map.Entry<String, InfoPage>> cleanupFilter = new RemoveFilter<Map.Entry<String, InfoPage>>()
	{
		@Override
		public boolean remove(Map.Entry<String, InfoPage> entry)
		{ return entry.getValue().childPages.isEmpty() && entry.getValue().getUnformattedText().trim().isEmpty(); }
	};
	
	public InfoPage parent = null;
	private ITextComponent title;
	public final List<InfoTextLine> text;
	public final LinkedHashMap<String, InfoPage> childPages;
	public InfoPageTheme theme;
	
	public InfoPage(String id)
	{
		super(id);
		text = new ArrayList<>();
		childPages = new LinkedHashMap<>();
	}
	
	public InfoPage setTitle(ITextComponent c)
	{
		title = c;
		return this;
	}
	
	public InfoPage setParent(InfoPage c)
	{
		parent = c;
		return this;
	}
	
	public InfoPage getOwner()
	{
		if(parent == null) { return this; }
		return parent.getOwner();
	}
	
	public void println(ITextComponent c)
	{
		if(c == null) { text.add(null); }
		else if(c instanceof TextComponentString && c.getStyle().isEmpty() && c.getSiblings().isEmpty())
		{ printlnText(((TextComponentString) c).getText()); }
		else { text.add(new InfoExtendedTextLine(this, c)); }
	}
	
	public void printlnText(String s)
	{ text.add((s == null || s.isEmpty()) ? null : new InfoTextLine(this, s)); }
	
	public String getUnformattedText()
	{
		if(text.isEmpty()) { return ""; }
		StringBuilder sb = new StringBuilder();
		int s = text.size();
		for(int i = 0; i < s; i++)
		{
			InfoTextLine c = text.get(i);
			
			if(c == null || c.getText() == null) { sb.append('\n'); }
			else
			{
				try { sb.append(c.getText().getUnformattedText()); }
				catch(Exception ex) { ex.printStackTrace(); }
			}
			
			if(i != s - 1) { sb.append('\n'); }
		}
		return sb.toString();
	}
	
	public void addSub(InfoPage c)
	{
		childPages.put(c.getID(), c);
		c.setParent(this);
	}
	
	public ITextComponent getTitleComponent()
	{ return title == null ? new TextComponentString(getID()) : title; }
	
	public InfoPage getSub(String id)
	{
		InfoPage c = childPages.get(id);
		if(c == null)
		{
			c = new InfoPage(id);
			c.setParent(this);
			childPages.put(id, c);
		}
		
		return c;
	}
	
	public void clear()
	{
		text.clear();
		childPages.clear();
	}
	
	public void cleanup()
	{
		for(InfoPage c : childPages.values()) c.cleanup();
		LMMapUtils.removeAll(childPages, cleanupFilter);
	}
	
	public void sortAll()
	{
		LMMapUtils.sortMap(childPages, new Comparator<Map.Entry<String, InfoPage>>()
		{
			@Override
			public int compare(Map.Entry<String, InfoPage> o1, Map.Entry<String, InfoPage> o2)
			{ return o1.getValue().compareTo(o2.getValue()); }
		});
		
		for(InfoPage c : childPages.values()) c.sortAll();
	}
	
	public void copyFrom(InfoPage c)
	{
		for(InfoTextLine l : c.text)
		{
			text.add(l == null ? null : l.copy(this));
		}
		
		for(InfoPage p : c.childPages.values())
		{
			InfoPage p1 = new InfoPage(p.getID());
			p1.copyFrom(p);
			addSub(p1);
		}
	}
	
	public InfoPage copy()
	{
		InfoPage page = new InfoPage(getID());
		page.fromJson(getSerializableElement());
		return page;
	}
	
	public InfoPage getParentTop()
	{
		if(parent == null) { return this; }
		return parent.getParentTop();
	}
	
	@Override
	public JsonElement getSerializableElement()
	{
		JsonObject o = new JsonObject();
		
		if(title != null) { o.add("N", JsonHelper.serializeICC(title)); }
		
		if(!text.isEmpty())
		{
			JsonArray a = new JsonArray();
			for(InfoTextLine c : text)
				a.add(c == null ? JsonNull.INSTANCE : c.getSerializableElement());
			o.add("T", a);
		}
		
		if(!childPages.isEmpty())
		{
			JsonObject o1 = new JsonObject();
			for(InfoPage c : childPages.values())
				o1.add(c.getID(), c.getSerializableElement());
			o.add("S", o1);
		}
		
		if(theme != null)
		{
			JsonElement e = theme.getSerializableElement();
			
			if(e != null)
			{
				o.add("C", e);
			}
		}
		
		return o;
	}
	
	@Override
	public void fromJson(JsonElement e)
	{
		clear();
		
		if(e == null || !e.isJsonObject()) { return; }
		JsonObject o = e.getAsJsonObject();
		
		title = o.has("N") ? JsonHelper.deserializeICC(o.get("N")) : null;
		
		if(o.has("T"))
		{
			JsonArray a = o.get("T").getAsJsonArray();
			for(int i = 0; i < a.size(); i++)
				text.add(InfoTextLine.get(this, a.get(i)));
		}
		
		if(o.has("S"))
		{
			JsonObject o1 = o.get("S").getAsJsonObject();
			
			for(Map.Entry<String, JsonElement> entry : o1.entrySet())
			{
				InfoPage c = new InfoPage(entry.getKey());
				c.setParent(this);
				c.fromJson(entry.getValue());
				childPages.put(c.getID(), c);
			}
		}
		
		if(o.has("C"))
		{
			theme = new InfoPageTheme();
			theme.fromJson(o.get("C"));
		}
		else
		{
			theme = null;
		}
	}
	
	public MessageDisplayInfo displayGuide(EntityPlayerMP ep)
	{
		MessageDisplayInfo m = new MessageDisplayInfo(this);
		if(ep != null && !(ep instanceof FakePlayer)) { m.sendTo(ep); }
		return m;
	}
	
	public final InfoPageTheme getTheme()
	{ return (theme == null) ? ((parent == null) ? InfoPageTheme.DEFAULT : parent.getTheme()) : theme; }
	
	@SideOnly(Side.CLIENT)
	public final void refreshGuiTree(GuiInfo gui)
	{
		refreshGui(gui);
		for(InfoPage p : childPages.values())
			p.refreshGuiTree(gui);
	}
	
	@SideOnly(Side.CLIENT)
	public void refreshGui(GuiInfo gui)
	{
	}
	
	@SideOnly(Side.CLIENT)
	public ButtonLM createSpecialButton(GuiInfo gui)
	{ return null; }
	
	@SideOnly(Side.CLIENT)
	public ButtonInfoPage createButton(GuiInfo gui)
	{ return new ButtonInfoPage(gui, this, null); }
	
	public String getFullID()
	{
		if(parent == null) { return getID(); }
		return parent.getFullID() + '.' + getID();
	}
	
	public String getPath()
	{
		if(parent == null) { return getID(); }
		return parent.getFullID() + '/' + getID();
	}
	
	public void loadText(List<String> list) throws Exception
	{
		for(JsonElement e : LMJsonUtils.deserializeText(list))
		{
			text.add(InfoTextLine.get(this, e));
		}
	}
}