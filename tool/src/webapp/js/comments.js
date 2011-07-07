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
	
	var pageToRequest = $(link).parent().children(".to-load").attr("href");
	
	$(link).parents(".replaceWithComments").load(pageToRequest);
	
	setMainFrameHeight(window.name);
}

function performHighlight() {
	$(".highlight-comment").effect("highlight", {}, 4000);
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
	$(commentToReload).load(deleteDialogCommentURL);
	//$("#delete-dialog").parents(".replaceWithComments").load($(dialog).children(".delete-dialog-comment-url").text());
	setMainFrameHeight(window.name);
	$("#delete-dialog").dialog("close");
	$("#delete-comment-confirm").text(originalDeleteDialogText);
}
