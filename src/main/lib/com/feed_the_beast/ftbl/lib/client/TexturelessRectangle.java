package com.feed_the_beast.ftbl.lib.client;

import com.feed_the_beast.ftbl.api.gui.IDrawableObject;
import com.feed_the_beast.ftbl.lib.Color4I;
import com.feed_the_beast.ftbl.lib.gui.GuiHelper;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Created by LatvianModder on 24.02.2017.
 */
public class TexturelessRectangle implements IDrawableObject
{
    public final Color4I color, lineColor;
    public boolean roundEdges = false;

    public TexturelessRectangle(Color4I col)
    {
        color = new Color4I(true, col);
        lineColor = new Color4I(true, Color4I.NONE);
    }

    public TexturelessRectangle setLineColor(Color4I col)
    {
        lineColor.set(col);
        return this;
    }

    public TexturelessRectangle setRoundEdges(boolean v)
    {
        roundEdges = v;
        return this;
    }

    public TexturelessRectangle copy()
    {
        TexturelessRectangle t = new TexturelessRectangle(color);
        t.lineColor.set(lineColor);
        t.roundEdges = roundEdges;
        return t;
    }

    @Override
    public void draw(int x, int y, int w, int h, Color4I col)
    {
        Color4I c = col.hasColor() ? col : color;

        if(roundEdges || lineColor.hasColor())
        {
            if(c.hasColor())
            {
                GuiHelper.drawBlankRect(x + 1, y + 1, w - 2, h - 2, c);
            }

            GuiHelper.drawHollowRect(x, y, w, h, lineColor.hasColor() ? lineColor : c, roundEdges);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        else if(c.hasColor())
        {
            GuiHelper.drawBlankRect(x, y, w, h, c);
        }
    }
}