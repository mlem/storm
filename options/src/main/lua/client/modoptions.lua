
require "OptionScreens/MainOptions"
require "ISUI/ISPanel"

local FONT_HGT_SMALL = getTextManager():getFontHeight(UIFont.Small)
local FONT_HGT_MEDIUM = getTextManager():getFontHeight(UIFont.Medium)
--

function obtainToken(button)
    return obtainTwitchToken;
end

local old_options_create = MainOptions.create
function MainOptions:create()
    old_options_create(self)

    -- check for functions
    if not (self.gameOptions and self.addPage and MainScreen.instance) then
        print('ERROR(options): lack of functions!')
        print(self.gameOptions, self.addPage, MainScreen.instance)
        return
    end

    local fontHgtSmall = FONT_HGT_SMALL
    local fontHgtMedium = FONT_HGT_MEDIUM
    --

    do
        local opt = self.gameOptions:get('vsync') or self.gameOptions:get('resolution')
                or self.gameOptions:get('soundVolume') or self.gameOptions:get('language')
        if not opt then
            tt = self.gameOptions
            return print('ERROR(options): no base option!','vsync')
        end
        GameOption = getmetatable(opt)
    end

    ----- Twitch talking enemies Page -----
    self:addPage("Storm")
    y = 20;
    x = 64

    label = ISLabel:new(x, y, fontHgtSmall, "some tipps", 1, 1, 1, 1, UIFont.Small, true)
    label:initialise()
    self.mainPanel:addChild(label)

    local debugTickBox = ISTickBox:new(x + 20, label:getY() + label:getHeight() + 10, 200, 20, "Debug")

    debugGameOption = GameOption:new('debugTwitchTalkingEnemies', debugTickBox)
    function debugGameOption.toUI(self)
        local box = self.control
        box:setSelected(0, getCore():getOptionDoWindSpriteEffects())
    end
    function debugGameOption.apply(self)
        local box = self.control
        getCore():setOptionDoWindSpriteEffects(box:isSelected(0))
    end
    self.gameOptions:add(debugGameOption)

    y = debugTickBox:getY() + debugTickBox:getHeight()

    local panel = ISPanel:new(x, y, self.width / 2 - x, 100)
    panel:noBackground()
    self.mainPanel:addChild(panel)

    local btn = ISButton:new(0, 10, 200, fontHgtSmall + 2 * 2, "grant twitch access to this mod", self, obtainToken())
    btn:initialise()
    btn:instantiate()
    panel:addChild(btn)

    y = btn:getY() + btn:getHeight()

    local panel = ISPanel:new(self.width / 2, 20, (self.width - 64 - (self.width / 2)), self.mainPanel.height - 20 - 20)
    panel:setAnchorRight(true)
    panel:setAnchorBottom(true)
    panel.drawBorder = true
    panel.mainOptions = self
    panel:initialise()
    self.mainPanel:addChild(panel)

    self.mainOptions:addTextPane(self.width / 2, 20, 20, 20)
    self.mainPanel:insertNewLineOfButtons(debugTickBox)
    self.mainPanel:insertNewLineOfButtons(btn)

    local clockFmt = self:addCombo(splitpoint, y, comboWidth, 20, getText("UI_optionscreen_clock_format"), { getText("UI_optionscreen_clock_month_day"), getText("UI_optionscreen_clock_day_month") }, 1)

    gameOption = GameOption:new('clockFormat', clockFmt)
    function gameOption.toUI(self)
        local box = self.control
        box.selected = getCore():getOptionClockFormat()
    end
    function gameOption.apply(self)
        local box = self.control
        if box.options[box.selected] then
            getCore():setOptionClockFormat(box.selected)
        end
    end
    self.gameOptions:add(gameOption)
end

