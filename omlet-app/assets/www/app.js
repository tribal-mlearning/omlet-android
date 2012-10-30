if (hasInjectedAppScript != true) {
	document.write('<script src="' + jkoRelativeParentPath + 'app_real.js" type="text/javascript" charset="utf-8"></script>');
	
	hasInjectedAppScript = true;
}