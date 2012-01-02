// Stuff for UI Changes
var toclick;
var winW, winH;

winH = $(parent.document).height();
winW = $(parent.document).width();

var picker = picker();

$('.add-forum-link').click(function(event){
	if (!picker) return true;
	
	toclick = '.add-forum-link';
	event.preventDefault();
	closeDropdown();
	var pageToRequest = $(this).attr("href");
	var text = $(this).text();
//		$('.pickertitle').text(text);
	loadpicker(pageToRequest);
	$('.picker').css("visibility","visible");
	return false;
});

$('.add-assignment-link').click(function(event){
	if (!picker) return true;
	
	toclick = '.add-assignment-link';
	event.preventDefault();
	closeDropdown();
	var pageToRequest = $(this).attr("href");
	var text = $(this).text();
//		$('.pickertitle').text(text);
	loadpicker(pageToRequest);
	$('.picker').css("visibility","visible");
	return false;
});

$('.add-quiz-link').click(function(event){
	if (!picker) return true;

	toclick = '.add-quiz-link';
	event.preventDefault();
	closeDropdown();
	var pageToRequest = $(this).attr("href");
	var text = $(this).text();
//		$('.pickertitle').text(text);
	loadpicker(pageToRequest);
	$('.picker').css("visibility","visible");
	return false;
});


$('#mm-choose').click(function(event){
	if (!picker) return true;

	event.preventDefault();
	$('#add-multimedia-dialog').dialog('close');
	var pageToRequest = $(this).attr("href");
	if (window == window.top) {
		// this is where i remove the mobile version stuff
	}
	var text = $(this).text();
//		$('.pickertitle').text(text);
	$('#ipickerframe').attr('src',pageToRequest);
	$('#ipickerframe').load();
	setTimeout(framesize, 250);
	setTimeout(framesize, 1000);
	setTimeout(framesize, 2000);
	$('.ipicker').css("visibility","visible");
});

// this will also be called by child pages to update the div
function loadpicker(address) {
	$('#pickerdiv').load(address);
	setTimeout(divsize, 250);
	setTimeout(divsize, 1000);
	setTimeout(divsize, 2000);
	return false;
}

function picker() {
	if (window == window.top && $(window).height() < 600) return false;	
	
	return true;
}

function divsize() {
		var port = $('#pickerdiv .portletBody');
		
		var h = port.height();
		$('#pickerid').height(h + 45);
		$('#pickerdiv').height(h + 15);
		checksize($('#pickerid'));
		return false;
}


function framesize() {
	var min = $('#ipickerframe').height();
	var h = $('#ipickerframe').contents().height();
	$('#ipickerid').height(h + 45);
	$('#ipickerframe').height(h);
	checksize($('#ipickerid'));
	return false;
}

$(document).keyup(function(e) {
    if (e.which == 27) hidepicker();
	return false;
});
 
$('.picker-x').click(function(){
	hidepicker();
	return false;
});

function hidepicker(refresh) {
	if (refresh == null || !refresh) {
		$('#ipickerframe').attr('src','about:blank');
		$('#ipickerframe').load();
		$('.picker, .ipicker').css("visibility","hidden");
		checksize();
	} else {
		window.location.reload();
	}
	return false;
}

var wind;
var intervalID = 0;
								        
function check() {
	if (wind.closed) {
		var alink;
		if (toclick == '.add-forum-link') {
			alink = $('#dropDownDiv').find(toclick);
		} else {
			alink = $('#toolbar').find(toclick);
		}
        alink.click();
        window.clearInterval(intervalID);
    }
	return false;
}

function newWin(target, event) {
	var i = target.indexOf("/portal");
	target = target.substring(i);
	if (!$.browser.msie) event.preventDefault();
    wind = window.open(target);
	intervalID = setInterval('check()', 2000);
    return false;
}

var mouse_is_inside;

$('.picker, .ipicker').hover(
function(){ 
	mouse_is_inside=true; 
}, 
function(){ 
	mouse_is_inside=false; 
});

$("body").mouseup(function(){ 
	if(!mouse_is_inside ){
		hidepicker();
	}
	return false;
});

function refresh() {
	$(window).reload();
}
