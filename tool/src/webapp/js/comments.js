/*
 * Special care was taken to ensure the ability to place multiple comments
 * tools on one page.  Please keep this in mind when changing this code.
 */

var commentItemToReload = null;
var deleteDialogCommentUrl = null;
var originalDeleteDialogText = null;

$(function() {
	if(sakai.editor.editors.ckeditor==undefined) {
		$(".evolved-box :not(textarea)").hide();
	}else {
		$(".evolved-box").hide();
	}
	
	$(".submitButton").hide();
	
	$.ajaxSetup ({
		cache: false
	});
	
	$(".replaceWithComments").each(function(index) {
		var pageToRequest = $(this).parent().parent().children(".commentsBlock").attr("href");
		$(this).load(pageToRequest);

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
	
	var pageToRequest = $(link).parent().parent().find(".to-load").attr("href");
	
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
	var evolved;
	
	if(sakai.editor.editors.ckeditor==undefined) {
		evolved = $(link).parents(".commentsDiv").find(".evolved-box :not(textarea)");
	}else {
		evolved = $(link).parents(".commentsDiv").find(".evolved-box");
	}
	
	evolved.show();
	
	if(sakai.editor.editors.ckeditor != undefined) {
		evolved.find("textarea").hide();
	}
	
	$(link).parents(".commentsDiv").find(".submitButton").show();
	
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

function edit(link, id) {
	var evolved = $(link).parents(".commentsDiv").find(".evolved-box");
	
	if(sakai.editor.editors.ckeditor==undefined) {
		FCKeditorAPI.GetInstance(evolved.children("textarea").attr("name")).SetHTML($(link).parent().children(".commentBody").html());
	}else {
		CKEDITOR.instances[evolved.children("textarea").attr("name")].setData($(link).parent().children(".commentBody").html());
	}
	
	$(link).parents(".commentsDiv").find(".comment-edit-id").val(id);
	$(link).parents(".commentsDiv").find(".submitButton").val("Edit Comment");
	
	switchEditors(link);
}
