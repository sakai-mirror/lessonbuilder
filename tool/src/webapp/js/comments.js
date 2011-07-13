/*
 * Special care was taken to ensure the ability to place multiple comments
 * tools on one page.  Please keep this in mind when changing this code.
 */

var commentItemToReload = null;
var deleteDialogCommentUrl = null;
var originalDeleteDialogText = null;

$(function() {
	$.ajaxSetup ({
		cache: false
	});
	
	$(".replaceWithComments").each(function(index) {
		var pageToRequest = $(this).parent().children(".commentsBlock").attr("href");
		$(this).load(pageToRequest, function() {
			if(sakai.editor.editors.ckeditor==undefined) {
				$(this).find(".evolved-box :not(textarea)").hide();
			}else {
				$(this).find(".evolved-box").hide();
			}
		});
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

function loadMore(link) {
	$.ajaxSetup ({
		cache: false
	});
	
	var pageToRequest = $(link).parent().children(".to-load").attr("href");
	
	$(link).parents(".replaceWithComments").load(pageToRequest, function() {
		if(sakai.editor.editors.ckeditor==undefined) {
			$(this).find(".evolved-box :not(textarea)").hide();
		}else {
			$(this).find(".evolved-box").hide();
		}
	});
	
	setMainFrameHeight(window.name);
}

function performHighlight() {
	$(".highlight-comment").effect("highlight", {}, 4000);
}

function switchEditors(link) {
	var regular = $(link).parent().find(".regular-text");
	
	var evolved = $(link).parent().find(".evolved-box");
	
	
	if($(link).parent().find(".regular-text").is(":visible")) {
		if(sakai.editor.editors.ckeditor==undefined) {
			//FCKeditorAPI.GetInstance("comment-text-area-evolved::input::1").SetHTML(regular.text(regular.val()).html());
			FCKeditorAPI.GetInstance(evolved.children("textarea").attr("name")).SetHTML(regular.text(regular.val()).html());
		}else {
			CKEDITOR.instances[evolved.children("textarea").attr("name")].setData(regular.text(regular.val()).html());
		}
	}
	
	if(sakai.editor.editors.ckeditor==undefined) {
		evolved = $(link).parent().find(".evolved-box :not(textarea)");
	}
	
	regular.toggle();
	evolved.toggle();
	
	if(sakai.editor.editors.ckeditor != undefined) {
		evolved.find("textarea").hide();
	}
	
	$(link).hide();
	
	setMainFrameHeight(window.name);
}

function deleteComment(link) {
	$.ajaxSetup ({
		cache: false
	});
	
	deleteDialogCommentURL = $(link).parent().children(".deleteComment").attr("href");

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
	$(commentToReload).load(deleteDialogCommentURL, function() {
		if(sakai.editor.editors.ckeditor==undefined) {
			$(this).find(".evolved-box :not(textarea)").hide();
		}else {
			$(this).find(".evolved-box").hide();
		}
	});
	//$("#delete-dialog").parents(".replaceWithComments").load($(dialog).children(".delete-dialog-comment-url").text());
	setMainFrameHeight(window.name);
	$("#delete-dialog").dialog("close");
	$("#delete-comment-confirm").text(originalDeleteDialogText);
}
