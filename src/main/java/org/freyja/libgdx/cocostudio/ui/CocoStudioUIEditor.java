/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freyja.libgdx.cocostudio.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import org.freyja.libgdx.cocostudio.ui.model.CCExport;
import org.freyja.libgdx.cocostudio.ui.model.CColor;
import org.freyja.libgdx.cocostudio.ui.model.FileData;
import org.freyja.libgdx.cocostudio.ui.model.ObjectData;
import org.freyja.libgdx.cocostudio.ui.model.timelines.CCTimelineActionData;
import org.freyja.libgdx.cocostudio.ui.model.timelines.CCTimelineData;
import org.freyja.libgdx.cocostudio.ui.model.timelines.CCTimelineFrame;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCButton;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCCheckBox;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCLabelAtlas;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCLayer;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCNode;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCPanel;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCProjectNode;
import org.freyja.libgdx.cocostudio.ui.parser.group.CCScrollView;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCImageView;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCLabel;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCLabelBMFont;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCLoadingBar;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCParticle;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCSlider;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCSpriteView;
import org.freyja.libgdx.cocostudio.ui.parser.widget.CCTextField;
import org.freyja.libgdx.cocostudio.ui.util.FontUtil;
import org.freyja.libgdx.cocostudio.ui.widget.TTFLabelStyle;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CocoStudio ui 解析器.根据CocoStudio的ui编辑器生成的json文件,创建出一个对应Group.
 * 本解析器还处于初级阶段,部分控件与属性不支持.
 *
 * @author i see
 * @email 121077313@qq.com
 * @wiki https://github.com/121077313/cocostudio-ui-libgdx/wiki
 * @tip https://github.com/121077313/cocostudio-ui-libgdx/wiki/疑难解答
 */
public class CocoStudioUIEditor {

    final String tag = CocoStudioUIEditor.class.getName();

    /**
     * json文件所在目录
     */
    protected String dirName;

    /**
     * 所有纹理
     */
    protected Collection<TextureAtlas> textureAtlas;

    /**
     * 控件集合
     */
    protected Map<String, Array<Actor>> actors;

    protected Map<Integer, Actor> actionActors;

    Map<String, Map<Actor, Action>> animations;

    //k: 控件ActionTag v: Action
    protected Map<Integer, Action> actionTagActionMap;

    /**
     * 字体集合
     */
    protected Map<String, FileHandle> ttfs;

    /**
     * BitmapFont集合,key:font.fnt
     */
    protected Map<String, BitmapFont> bitmapFonts;

    /**
     * 导出的json结构
     */
    protected CCExport export;

    protected Map<String, BaseWidgetParser> parsers;

    /**
     * 添加转换器
     */
    public void addParser(BaseWidgetParser parser) {
        parsers.put(parser.getClassName(), parser);
    }

    /**
     * 默认ttf字体文件
     */
    protected FileHandle defaultFont;

    /**
     * 不需要显示文字
     *
     * @param jsonFile
     * @param textureAtlas 资源文件,传入 null表示使用小文件方式加载图片
     */
    public CocoStudioUIEditor(FileHandle jsonFile,
                              Collection<TextureAtlas> textureAtlas) {
        this(jsonFile, null, null, null, textureAtlas);
    }

    /**
     * @param jsonFile     ui编辑成生成的json文件
     * @param textureAtlas 资源文件,传入 null表示使用小文件方式加载图片.
     * @param ttfs         字体文件集合
     * @param bitmapFonts  自定义字体文件集合
     * @param defaultFont  默认ttf字体文件
     */
    public CocoStudioUIEditor(FileHandle jsonFile,
                              Map<String, FileHandle> ttfs, Map<String, BitmapFont> bitmapFonts,
                              FileHandle defaultFont, Collection<TextureAtlas> textureAtlas) {
        this.textureAtlas = textureAtlas;
        this.ttfs = ttfs;
        this.bitmapFonts = bitmapFonts;
        this.defaultFont = defaultFont;
        parsers = new HashMap<String, BaseWidgetParser>();

        addParser(new CCButton());
        addParser(new CCCheckBox());
        addParser(new CCImageView());
        addParser(new CCLabel());
        addParser(new CCLabelBMFont());
        addParser(new CCPanel());
        addParser(new CCScrollView());
        addParser(new CCTextField());
        addParser(new CCLoadingBar());

        addParser(new CCLayer());

        addParser(new CCLabelAtlas());
        addParser(new CCSpriteView());
        addParser(new CCNode());

        addParser(new CCSlider());

        addParser(new CCParticle());
        addParser(new CCProjectNode());

        actors = new HashMap<String, Array<Actor>>();
        actionActors = new HashMap<Integer, Actor>();

        animations = new HashMap<String, Map<Actor, Action>>();

        actionTagActionMap = new HashMap<Integer, Action>();

        dirName = jsonFile.parent().toString();

        if (!dirName.equals("")) {
            dirName += File.separator;
        }
        String json = jsonFile.readString("utf-8");
        Json jj = new Json();
        jj.setIgnoreUnknownFields(true);
        export = jj.fromJson(CCExport.class, json);

        parseAction();
    }

    /**
     * 根据控件名字查找Actor
     *
     * @param name 控件名字
     * @return
     */
    public <T extends Actor> T findActor(String name) {
        Array<Actor> array = actors.get(name);
        if (array == null || array.size == 0) {
            return null;
        }
        return (T) array.get(0);
    }

    /**
     * 查找所有同名的控件
     */
    public Array<Actor> findActors(String name) {

        return actors.get(name);
    }

    /**
     * 根据json文件创建并返回Group
     *
     * @return
     */
    public Group createGroup() {
        Actor actor = parseWidget(null, export.getContent().getContent()
            .getObjectData());

        return (Group) actor;
    }

    /**
     * 查找动画
     */
    public Map<Actor, Action> getAction(String animationName) {

        return animations.get(animationName);
    }

    /**
     * 转换动作Action
     */
    void parseAction() {
        CCTimelineActionData ccTimelineActionData = export.getContent().getContent().getAnimation();
        float duration = ccTimelineActionData.getDuration();
        float speed = ccTimelineActionData.getSpeed();

        List<CCTimelineData> ccTimelineDatas = ccTimelineActionData.getTimelines();

        for (CCTimelineData ccTimelineData : ccTimelineDatas) {
            //位移动画
            if (ccTimelineData.getProperty().equals("Position")) {
                List<CCTimelineFrame> ccTimelineFrames = ccTimelineData.getFrames();

                SequenceAction sequenceAction = Actions.sequence();

                /*for (int i = 0; i < ccTimelineFrames.size(); i++) {
                    CCTimelineFrame ccTimelineFrame = ccTimelineFrames.get(i);

                }*/

                for (CCTimelineFrame ccTimelineFrame : ccTimelineFrames) {
                    Action action = Actions.moveTo(ccTimelineFrame.getX(), ccTimelineFrame.getY()
                        , speed
                            / duration *
                            ccTimelineFrame
                                .getFrameIndex(),
                        getInterpolation(ccTimelineFrame.getEasingData().getType()));

                    sequenceAction.addAction(action);
                }

                actionTagActionMap.put(ccTimelineData.getActionTag(), sequenceAction);
            }
        }

       /* CCAnimation animation = export.getContent().getContent().getAnimation();
        for (CCAction action : animation.getActionlist()) {

            List<CCActionNode> nodes = action.getActionnodelist();
            Map<Actor, Action> actions = new HashMap<Actor, Action>();
            for (CCActionNode node : nodes) {
                Actor actor = actionActors.get(node.getActionTag());
                List<CCActionFrame> frames = node.getActionframelist();
                // frameid 排序.
                SequenceAction sequenceAction = Actions.sequence();
                for (CCActionFrame frame : frames) {
                    Interpolation interpolation = getInterpolation(frame
                        .getTweenType());

                    float duration = 0;
                    // Starttime 会是一个类似
                    // 7.163279E-39的字符没办法直接转换Float,所以这里采用截取字符串的方法
                    int length = frame.getStarttime().indexOf("E");
                    if (length != -1) {
                        duration = Float.parseFloat(frame.getStarttime()
                            .substring(0, length));
                    } else {
                        duration = Float.parseFloat(frame.getStarttime());
                    }

                    Action moveTo = Actions.moveTo(frame.getPositionx(),
                        frame.getPositiony(), duration, interpolation);

                    Action scaleTo = Actions.scaleTo(frame.getScalex(),
                        frame.getScaley(), duration, interpolation);

                    Action color = Actions.color(new Color(
                            frame.getColorr() / 255.0f,
                            frame.getColorg() / 255.0f,
                            frame.getColorb() / 255.0f,
                            frame.getOpacity() / 255.0f), duration,
                        interpolation);

                    Action rotateTo = Actions.rotateTo(frame.getRotation(),
                        duration, interpolation);

                    sequenceAction.addAction(Actions.parallel(moveTo, scaleTo,
                        color, rotateTo));
                }

                actions.put(actor, sequenceAction);
            }

            animations.put(action.getName(), actions);
        }*/

    }

    public Map<Integer, Action> getActionTagActionMap() {
        return actionTagActionMap;
    }

    /**
     * @param tweenType
     * @return
     * @author tian
     * 根据传入的值返回插值类型
     */
    public Interpolation getInterpolation(int tweenType) {
        switch (tweenType) {
            case 0:
                return Interpolation.linear;
            case 1:
                return Interpolation.sineIn;
            case 2:
                return Interpolation.sineOut;
            case 3:
                return Interpolation.sine;
            case 4:
                return Interpolation.linear;//不支持Quad_EaseIn
            case 5:
                return Interpolation.linear;//不支持Quad_EaseOut
            case 6:
                return Interpolation.linear;//不支持Quad_EaseInOut
            case 7:
                return Interpolation.linear;//不支持Cubic_EaseIn
            case 8:
                return Interpolation.linear;//不支持Cubic_EaseOut
            case 9:
                return Interpolation.linear;//不支持Cubic_EaseInOut
            case 10:
                return Interpolation.linear;//不支持Quart_EaseIn
            case 11:
                return Interpolation.linear;//不支持Quart_EaseOut
            case 12:
                return Interpolation.linear;//不支持Quart_EaseInOut
            case 13:
                return Interpolation.linear;//不支持Quint_EaseIn
            case 14:
                return Interpolation.linear;//不支持Quint_EaseOut
            case 15:
                return Interpolation.linear;//不支持Quint_EaseInOut
            case 16:
                return Interpolation.exp10In;
            case 17:
                return Interpolation.exp10Out;
            case 18:
                return Interpolation.exp10;
            case 19:
                return Interpolation.circleIn;
            case 20:
                return Interpolation.circleOut;
            case 21:
                return Interpolation.circle;
            case 22:
                return Interpolation.elasticIn;
            case 23:
                return Interpolation.elasticOut;
            case 24:
                return Interpolation.elastic;
            case 25:
                return Interpolation.linear;//不支持Back_EaseIn
            case 26:
                return Interpolation.linear;//不支持Back_EaseOut
            case 27:
                return Interpolation.linear;//不支持Back_EaseInOut
            case 28:
                return Interpolation.bounceIn;
            case 29:
                return Interpolation.bounceOut;
            case 30:
                return Interpolation.bounce;
        }

        return null;
    }

    protected TextureRegion findRegion(String name) {
        for (TextureAtlas ta : textureAtlas) {
            if (ta == null) {
                continue;
            }
            TextureRegion tr = ta.findRegion(name);
            if (tr != null) {
                return tr;
            }
        }
        return null;
    }

    protected TextureRegion findRegion(String name, int index) {
        for (TextureAtlas ta : textureAtlas) {
            if (ta == null) {
                continue;
            }
            TextureRegion tr = ta.findRegion(name, index);
            if (tr != null) {
                return tr;
            }
        }
        return null;
    }

    public String findParticePath(String name) {
        if (name == null || name.equals("")) {
            return null;
        }

        return (dirName + name);
    }

    /**
     * 获取材质
     *
     * @param option
     * @param name
     * @return
     */
    public TextureRegion findTextureRegion(ObjectData option, String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        TextureRegion tr = null;

        if (textureAtlas == null || textureAtlas.size() == 0) {// 不使用合并纹理
            tr = new TextureRegion(new Texture(Gdx.files.internal(dirName
                + name)));
        } else {

            // try {
            // String[] arr = name.split("\\/");
            //
            // name = name.substring(arr[0].length() + 1,
            // name.length() - 4);
            // } catch (Exception e) {
            // error(option, "名称不符合约定,无法解析.请查看github项目wiki");
            // }
            //

            try {
                String[] arr = name.split("\\/");
                if (arr.length == 1) {
                    // support same folder with json file
                    // add by @xiaozc

                    name = name.substring(0, name.length() - 4);
                } else {
                    name = name.substring(arr[0].length() + 1,
                        name.length() - 4);
                }
            } catch (Exception e) {
                error(option, "资源名称不符合约定,无法解析.请查看github项目wiki第十条");
            }

            // 考虑index下标

            if (name.indexOf("_") == -1) {
                tr = findRegion(name);
            } else {
                try {
                    int length = name.lastIndexOf("_");

                    Integer index = Integer.parseInt(name.substring(length + 1,
                        name.length()));
                    // 这里可能报错,属于正常,因为会出现 xx_xx名字的资源而不是xx_2这种

                    name = name.substring(0, length);

                    tr = findRegion(name, index);

                } catch (Exception e) {
                    tr = findRegion(name);
                }
            }
        }
        if (tr == null) {
            debug(option, "找不到纹理");
            return null;
        }

        if (option.isFlipX() || option.isFlipY()) {

            if (textureAtlas == null) {
                tr.flip(option.isFlipX(), option.isFlipY());
            } else {
                tr = new TextureRegion(tr);
                tr.flip(option.isFlipX(), option.isFlipY());
            }
        }

        return tr;
    }

    /**
     * .9文件生成
     *
     * @param option
     * @param name
     * @return
     * @author wujj
     */
    // public NinePatch findNinePatch(ObjectData option, String name) {
    // if (name == null || name.equals("")) {
    // return null;
    // }
    //
    // NinePatch tr = null;
    // if (textureAtlas == null || textureAtlas.size() == 0) {// 不使用合并纹理
    // tr = new NinePatch(new Texture(Gdx.files.internal(dirName + name)),
    // option.getCapInsetsX(), option.getCapInsetsX()
    // + option.getCapInsetsWidth(),
    // option.getCapInsetsY(), option.getCapInsetsY()
    // + option.getCapInsetsHeight());
    // } else {
    // name = name.substring(0, name.indexOf("."));
    // // 考虑index下标
    //
    // if (name.indexOf("_") == -1) {
    // for (TextureAtlas atlas : textureAtlas) {
    // tr = atlas.createPatch(name);
    // if (tr != null) {
    // break;
    // }
    // }
    // } else {
    // try {
    // // 不支持同名索引查找
    // int length = name.lastIndexOf("_");
    // Integer index = Integer.parseInt(name.substring(length + 1,
    // name.length()));
    // name = name.substring(0, length);
    // for (TextureAtlas atlas : textureAtlas) {
    // tr = atlas.createPatch(name);
    // if (tr != null) {
    // break;
    // }
    // }
    // } catch (Exception e) {
    // for (TextureAtlas atlas : textureAtlas) {
    // tr = atlas.createPatch(name);
    // if (tr != null) {
    // break;
    // }
    // }
    // }
    // }
    // }
    // if (tr == null) {
    // debug(option, "找不到纹理");
    // }
    // // 不支持翻转和镜像
    // return tr;
    // }
    public Drawable findDrawable(ObjectData option, FileData fileData) {
        //显示Default
//		if (fileData == null || "Default".equals(fileData.getType())) {// 默认值不显示
//			return null;
//		}
        //显示Default
        if (fileData == null) {// 默认值不显示
            return null;
        }

        return findDrawable(option, fileData.getPath());
    }

    public Drawable findDrawable(ObjectData option, String name) {

        if (option.isScale9Enable()) {// 九宫格支持
            NinePatch np = new NinePatch(findTextureRegion(option, name),
                option.getScale9OriginX(), option.getScale9OriginY(),
                option.getScale9Width(), option.getScale9Height());

            if (np == null) {
                return null;
            }

            np.setColor(getColor(option.getCColor(), option.getAlpha()));
            return new NinePatchDrawable(np);
        }

        TextureRegion tr = findTextureRegion(option, name);

        if (tr == null) {
            return null;
        }

        //TextureRegionDrawable textureRegionDrawable = new TextureRegionDrawable(tr);
        // textureRegionDrawable.tint(getColor(option.getCColor(), option.getAlpha()));
        return new TextureRegionDrawable(tr);
    }

    public void debug(String message) {
        Gdx.app.debug(tag, message);
    }

    public void debug(ObjectData option, String message) {
        Gdx.app.debug(tag, "控件: " + option.getCtype() + "," + option.getName()
            + " " + message);
    }

    public void error(String message) {
        Gdx.app.error(tag, message);
    }

    public void error(ObjectData option, String message) {
        Gdx.app.error(tag, "控件: " + option.getName() + " " + message);
    }

    /***
     * 解析节点,创建控件
     *
     * @param node
     * @return
     */
    public Actor parseWidget(Group parent, ObjectData widget) {

        String className = widget.getCtype();
        BaseWidgetParser parser = parsers.get(className);

        if (parser == null) {
            debug(widget, "not support Widget:" + className);
            return null;
        }
        Actor actor = parser.parse(this, widget);

        actor = parser.commonParse(this, widget, parent, actor);

        return actor;
    }

    /**
     * 获取BitmapFont
     */
    public BitmapFont getBitmapFont(ObjectData option) {
        BitmapFont font = null;
        if (bitmapFonts != null) {
            font = bitmapFonts.get(option.getLabelBMFontFile_CNB().getPath());
        } else {
            font = new BitmapFont(Gdx.files.internal(dirName
                + option.getLabelBMFontFile_CNB().getPath()));
        }

        if (font == null) {
            debug(option, "BitmapFont字体:"
                + option.getLabelBMFontFile_CNB().getPath() + " 不存在");
            font = new BitmapFont();
        }
        return font;
    }

    public Color getColor(CColor c, int alpha) {
        Color color = null;
        if (c == null || c.getR() + c.getG() + c.getB() == 0) {
            color = new Color(Color.WHITE);
        } else {
            color = new Color();
            color.a = 1;
            color.r = c.getR() / 255f;
            color.g = c.getG() / 255f;
            color.b = c.getB() / 255f;
        }

        if (alpha != 0) {
            color.a = alpha / 255f;
        }

        return color;
    }

    /**
     * 创建LabelStyle
     *
     * @param option
     * @return
     */
    public TTFLabelStyle createLabelStyle(ObjectData option, String text,
                                          Color color) {

        FileHandle fontFile = null;
        if (ttfs != null && option.getFontResource() != null) {
            fontFile = ttfs.get(option.getFontResource().getPath());
        }

        if (fontFile == null) {// 使用默认字体文件
            fontFile = defaultFont;
        }

        // Color textColor = getColor(option.getTextColor());
        // if (option.getAlpha() != 0) {
        // textColor.a = option.getAlpha() / 255f;
        // }

        if (fontFile == null) {
            debug(option, "ttf字体:" + option.getFontResource().getPath()
                + " 不存在,使用默认字体");
        }

        BitmapFont font = FontUtil.createFont(fontFile, text,
            option.getFontSize());

        return new TTFLabelStyle(new LabelStyle(font, color), fontFile,
            option.getFontSize());
    }

    /**
     * 创建LabelStyle的BitmapFont
     *
     * @param option
     * @return
     */
    public BitmapFont createLabelStyleBitmapFint(ObjectData option, String text,
                                                 Color color) {

        FileHandle fontFile = null;
        if (ttfs != null && option.getFontResource() != null) {
            fontFile = ttfs.get(option.getFontResource().getPath());
        }

        if (fontFile == null) {// 使用默认字体文件
            fontFile = defaultFont;
        }

        // Color textColor = getColor(option.getTextColor());
        // if (option.getAlpha() != 0) {
        // textColor.a = option.getAlpha() / 255f;
        // }

        if (fontFile == null) {
            try {
                debug(option, "ttf字体:" + option.getFontResource().getPath()
                    + " 不存在,使用默认字体");
            } catch (Exception e) {
                //e.printStackTrace();
                debug(option, "不存在字体,使用默认字体");
            }
        }

        BitmapFont font = FontUtil.createFont(fontFile, text,
            option.getFontSize(), color);

        font.setColor(color);

        return font;
    }

    public Map<String, Array<Actor>> getActors() {
        return actors;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public Map<String, FileHandle> getTtfs() {
        return ttfs;
    }

    public void setTtfs(Map<String, FileHandle> ttfs) {
        this.ttfs = ttfs;
    }

    public Map<String, BitmapFont> getBitmapFonts() {
        return bitmapFonts;
    }

    public void setBitmapFonts(Map<String, BitmapFont> bitmapFonts) {
        this.bitmapFonts = bitmapFonts;
    }

    public void setActors(Map<String, Array<Actor>> actors) {
        this.actors = actors;
    }

    public Map<Integer, Actor> getActionActors() {
        return actionActors;
    }

    public void setActionActors(Map<Integer, Actor> actionActors) {
        this.actionActors = actionActors;
    }

    public FileHandle getDefaultFont() {
        return defaultFont;
    }

    public Collection<TextureAtlas> getTextureAtlas() {
        return textureAtlas;
    }
}
