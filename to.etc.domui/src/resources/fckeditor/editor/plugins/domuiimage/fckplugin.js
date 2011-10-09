﻿/*
 * vmijic 20111006 - DomUI Image plugin for FCKeditor
 *  
 * This plugin register Toolbar items for command that would be used to implement custom image browsing integrated with Domui.
 */

/***
 * Create blank command, seems useless but needs to exits according to api...
 */
var DomuiImage_command = function()
{

};

/***
 * Add Execute prototype
 */
DomuiImage_command.prototype.Execute = function()
{
    // get FCKeditor name, that is actually set as ID within domui
	var fckName = FCK.Name;

    // call domui
	window.parent.WebUI.scall(fckName, "FCKIMAGE", {
		_fckId : fckName
	});
};

/***
 * Method that is exected when some image url needs to be added to editor (usually as rendered response from domui handler)
 */
DomuiImage_addImage = function(fckName, imageUrl)
{
	var oEditor = FCKeditorAPI.GetInstance(fckName);
	//oEditor.FCKUndo.SaveUndoStep() ;
	var oImage = oEditor.InsertElement('img') ;
    oImage.src = imageUrl;
};

Domui_fixLayout = function(fckId)
{
	if (window.parent.WebUI.isIE8orIE8c())
	{
		//20111009 vmijic FCKEditor has bug in IE8 -> it's inner iframe body height is not growing to specified dimension. 
		//We need to fix that manually -> desperate hours were spent to find a way to achive that :( 
		var fckIFrame = window.parent.document.getElementById(fckId + '___Frame');
		if (fckIFrame)
		{
			var tdtb = fckIFrame.contentWindow.document.getElementById('xToolbarSpace');
			if (tdtb)
			{
				fckIFrame.contentWindow.document.body.style.height = (fckIFrame.offsetHeight - tdtb.offsetHeight - 7)+'px';
				var tdea = fckIFrame.contentWindow.document.getElementById('xEditingArea'); 
				tdea.style.display = 'inline';
			}
		}
	};
};

/***
 * Add GetState prototype
 * - This is one of the lines I can't explain
 * Seems useless but needs to exits according to api...
 */
DomuiImage_command.prototype.GetState = function()
{
        return;
};

var oDomuiImage = new FCKToolbarButton( 'DomUI_Image', FCKLang['DomUI_Image'] ) ;
oDomuiImage.IconPath = FCKConfig.PluginsPath + 'domuiimage/image.gif' ;

//Register toolbar button
FCKToolbarItems.RegisterItem( 'DomUI_Image', oDomuiImage ) ; // 'DomUI_Image' is the name used in the Toolbar config.

//Register the related commands.
FCKCommands.RegisterCommand( 'DomUI_Image' , new DomuiImage_command()) ;