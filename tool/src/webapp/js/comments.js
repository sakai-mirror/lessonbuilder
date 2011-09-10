/*
 * Special care was taken to ensure the ability to place multiple comments
 * tools on one page.  Please keep this in mind when changing this code.
 */

var commentItemToReload = null;
var deleteDialogCommentUrl = null;
var originalDeleteDialogText = null;
var noEditor = true;

$(function() {
	
	noEditor = ($("div.evolved-box").size() == 0);

	if (noEditor) {
		$(".evolved-box").hide();
	}else if(sakai.editor.editors.ckeditor==undefined) {
		$(".evolved-box :not(textarea)").hide();
	}else {
		$(".evolved-box").hide();
	}
	
	$(".submitButton").hide().val(msg("simplepage.add-comment"));
	$(".cancelButton").hide().val(msg("simplepage.cancel_message"));
	
	$(".cancelButton").click(function() {
		switchEditors($(this), false);
		return false;
	});
	
	$.ajaxSetup ({
		cache: false
	});
	
	$(".replaceWithComments").each(function(index) {
		var pageToRequest = $(this).parent().parent().children(".commentsBlock").attr("href");
		// remove  pda/SITEID/. We don't want the portal to hack on this
		//pageToRequest = pageToRequest.replace(new RegExp("pda/[^/]*/",""),"");
                var i = pageToRequest.indexOf("Comment");
                pageToRequest = "/sakai-lessonbuildertool-tool/faces/" + pageToRequest.substring(i);
		$(this).load(pageToRequest, commentsLoaded);
		//$.PeriodicalUpdater(pageToRequest, {minTimeout:5000, maxTimeout:120000}, function(data) {
		//	$(".deleteLink").attr("title", msg("simplepage.comment_delete"));
		//	$(".editLink").attr("title", msg("simplepage.edit-comment"));
		//	$(".replaceWithComments").html(data);
		//});
	});
	
	$("#delete-dialog").dialog({
		autoOpen: false,
		width: 400,
		modal: false,
		resizable: false,
		buttons: {
			"Cancel": function() {
				if(originalDeleteDialogText != null) {
					$("#delete-comment-confirm").text(originalDeleteDialogText);
				}
				
				$(this).dialog("close");
			},
			
			"Delete Comment": function() {
				confirmDelete();
			}
		}
	});
});

function commentsLoaded() {
	$(".deleteLink").attr("title", msg("simplepage.comment_delete"));
	$(".editLink").attr("title", msg("simplepage.edit-comment"));
	setMainFrameHeight(window.name);
}

function loadMore(link) {
	$.ajaxSetup ({
		cache: false
	});
	
	var pageToRequest = $(link).parent().parent().find(".to-load").attr("href");
	var i = pageToRequest.indexOf("Comment");
	pageToRequest = "/sakai-lessonbuildertool-tool/faces/" + pageToRequest.substring(i);
	
	$(link).parents(".replaceWithComments").load(pageToRequest, commentsLoaded);
	
	setMainFrameHeight(window.name);
}

function performHighlight() {
	$(".highlight-comment").effect("highlight", {}, 4000);
}

function switchEditors(link, show) {
	if(show==undefined) show = true;
	
	var evolved;
	
	if (noEditor) {
		evolved = $(link).parents(".commentsDiv").find(".evolved-box");
		evolved.css('width', '600px');
		evolved.css('height', '175px');
	}else if(sakai.editor.editors.ckeditor==undefined) {
		evolved = $(link).parents(".commentsDiv").find(".evolved-box :not(textarea)");
	}else {
		evolved = $(link).parents(".commentsDiv").find(".evolved-box");
	}
	
	if(show) {
		evolved.show();

		if (noEditor) {
		    // the submit expects HTML, so stick BRs at every newline
		    var submit = $(link).parents(".commentsDiv").find(".submitButton");
		   submit.click(function(link) {
			    var text = $(this).parents(".commentsDiv").find(".evolved-box");
			    text.val(text.val().replace(/\r\n/g, "<br/>\n").replace(/[\r\n]/g, "<br/>\n"));
			    return true;
			});
		}
		
		$(link).parents(".commentsDiv").find(".submitButton").show();
		$(link).parents(".commentsDiv").find(".cancelButton").show();
		
		$(link).parents(".commentsDiv").find(".switchLink").hide();
	}else {
		evolved.hide();
		
		if(!noEditor && sakai.editor.editors.ckeditor==undefined) {
			evolved = $(link).parents(".commentsDiv").find(".evolved-box");
		}
		
		if (noEditor) {
			evolved.val("");
		} else if(sakai.editor.editors.ckeditor==undefined) {
			FCKeditorAPI.GetInstance(evolved.children("textarea").attr("name")).SetHTML("");
		}else {
			CKEDITOR.instances[evolved.children("textarea").attr("name")].setData("");
		}
		
		$(link).parents(".commentsDiv").find(".submitButton").hide().val(msg("simplepage.add-comment"));
		$(link).parents(".commentsDiv").find(".cancelButton").hide().val(msg("simplepage.cancel_message"));
		
		$(link).parents(".commentsDiv").find(".switchLink").show();
		$(link).parents(".commentsDiv").find(".comment-edit-id").val("");
	}
	
	if(!noEditor && sakai.editor.editors.ckeditor != undefined) {
		evolved.find("textarea").hide();
	}
	
	setMainFrameHeight(window.name);
	$(document).height($('body').height());
}

function deleteComment(link) {
	$.ajaxSetup ({
		cache: false
	});
	
	deleteDialogCommentURL = $(link).parent().children(".deleteComment").attr("href");
	var i = deleteDialogCommentURL.indexOf("Comment");
	deleteDialogCommentURL = "/sakai-lessonbuildertool-tool/faces/" + deleteDialogCommentURL.substring(i);

	//var dialog = $(link).parents(".replaceWithComments").find(".delete-dialog");
	//$("#delete-dialog").children(".delete-dialog-comment-url").text(pageToRequest);
	
	originalDeleteDialogText = $("#delete-comment-confirm").text();
	$("#delete-comment-confirm").text(
			originalDeleteDialogText.replace("{}", link.parents(".commentDiv").find(".author").text()));
	
	$("#delete-dialog").dialog("open");
	
	commentToReload = $(link).parents(".replaceWithComments");


	
	//$(link).parents(".replaceWithComments").load(pageToRequest);
	
	//setMainFrameHeight(window.name);
}

function confirmDelete() {
	$(commentToReload).load(deleteDialogCommentURL, commentsLoaded);
	//$("#delete-dialog").parents(".replaceWithComments").load($(dialog).children(".delete-dialog-comment-url").text());
	setMainFrameHeight(window.name);
	$("#delete-dialog").dialog("close");
	$("#delete-comment-confirm").text(originalDeleteDialogText);
}

function edit(link, id) {
	var evolved = $(link).parents(".commentsDiv").find(".evolved-box");
	
	if(noEditor) {
	    var body = $(link).parent().children(".commentBody");
	    // this should get the text out of the comment with newlines
	    // however innertext doesn't work on FF and I've heard rumors
	    // about IE 9. Hence provide a standard backup. It will handle
	    // newlines if they were entered via this text box, but 
	    // may not if entered in the editor
	    body = body.get(0).innerText || body.text();
	    evolved.val(body);

	} else if(sakai.editor.editors.ckeditor==undefined) {
		FCKeditorAPI.GetInstance(evolved.children("textarea").attr("name")).SetHTML($(link).parent().children(".commentBody").html());
	}else {
		CKEDITOR.instances[evolved.children("textarea").attr("name")].setData($(link).parent().children(".commentBody").html());
	}
	
	$(link).parents(".commentsDiv").find(".comment-edit-id").val(id);
	$(link).parents(".commentsDiv").find(".submitButton").val(msg("simplepage.edit-comment"));
	
	switchEditors(link);
}
